package com.atguigu.java.ai.langchain4j.tools;

import com.atguigu.java.ai.langchain4j.context.AuthenticatedRequestContext;
import com.atguigu.java.ai.langchain4j.context.SessionTokenRegistry;
import com.atguigu.java.ai.langchain4j.entity.Appointment;
import com.atguigu.java.ai.langchain4j.service.AppointmentService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
@Slf4j
public class AppointmentTools {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final Pattern BOOK_CONFIRMATION = Pattern.compile(
            "^(确认|确认预约|确认挂号|同意预约|请预约|现在预约)[。！!\\s]*$");
    private static final Pattern CANCEL_CONFIRMATION = Pattern.compile(
            "^(确认取消|确认取消预约|同意取消|请取消)[。！!\\s]*$");

    private final AppointmentService appointmentService;
    private final RestTemplate restTemplate;
    private final String serviceHospUrl;
    private final String gatewayUrl;

    @Autowired
    public AppointmentTools(AppointmentService appointmentService,
                            RestTemplateBuilder restTemplateBuilder,
                            @Value("${yygh.service-hosp-url:http://192.168.6.1:8201}") String serviceHospUrl,
                            @Value("${yygh.gateway-url:http://192.168.6.1:8222}") String gatewayUrl) {
        this.appointmentService = appointmentService;
        // 功能完善：远程调用设置连接和读取超时，避免 AI 对话线程无限等待。
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
        this.serviceHospUrl = normalizeBaseUrl(serviceHospUrl);
        this.gatewayUrl = normalizeBaseUrl(gatewayUrl);
    }

    /**
     * 获取当前请求的认证令牌。
     * 按优先级依次尝试：ThreadLocal → RequestContextHolder HTTP Header → SessionTokenRegistry(memoryId)。
     */
    private String currentToken(Long memoryId) {
        log.info("【Token诊断】开始获取 token, memoryId={}, thread={}", memoryId, Thread.currentThread().getName());
        String token = AuthenticatedRequestContext.getToken();
        log.info("【Token诊断】ThreadLocal token={}", StringUtils.hasText(token) ? token.substring(0, Math.min(8, token.length())) + "..." : "null");
        if (StringUtils.hasText(token)) {
            return token;
        }
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            log.info("【Token诊断】RequestContextHolder attrs={}", attrs != null);
            if (attrs != null) {
                jakarta.servlet.http.HttpServletRequest request = attrs.getRequest();
                token = request.getHeader("token");
                if (!StringUtils.hasText(token)) {
                    token = request.getHeader("X-Token");
                }
                log.info("【Token诊断】HTTP Header token={}", StringUtils.hasText(token) ? "有值" : "null");
                if (StringUtils.hasText(token)) {
                    return token.trim();
                }
            }
        } catch (Exception e) {
            log.warn("【Token诊断】RequestContextHolder 异常: {}", e.getMessage());
        }
        if (memoryId != null) {
            token = SessionTokenRegistry.get(memoryId);
            log.info("【Token诊断】SessionTokenRegistry token={}", StringUtils.hasText(token) ? token.substring(0, Math.min(8, token.length())) + "..." : "null");
            if (StringUtils.hasText(token)) {
                return token;
            }
        }
        log.warn("【Token诊断】所有来源都没拿到 token，返回 null");
        return null;
    }

    @Tool(name = "book_appointment", value = "预约挂号：根据参数查询号源。用户确认预约信息后，使用当前登录账号下匹配的就诊人创建平台正式订单。")
    public String bookAppointment(
            @ToolMemoryId Long memoryId,
            @P(value = "就诊人姓名") String username,
            @P(value = "证件号后四位；同名就诊人需要提供，只有一个同名就诊人时可不填", required = false) String idCardLast4,
            @P(value = "科室名称") String department,
            @P(value = "日期") String date,
            @P(value = "时间，可选值：上午、下午") String time,
            @P(value = "医生名称", required = false) String doctorName) {
        log.info("【预约工具】开始执行预约挂号，memoryId={}, username={}, department={}, date={}, time={}, doctorName={}",
                memoryId, username, department, date, time, doctorName);
        Appointment appointment = appointmentFromArguments(
                username, idCardLast4, department, date, time, doctorName);
        String validationMessage = validateAppointment(appointment);
        if (validationMessage != null) {
            log.info("【预约工具】参数校验失败：{}", validationMessage);
            return validationMessage;
        }
        String userMsg = AuthenticatedRequestContext.getUserMessage();
        boolean isConfirm = BOOK_CONFIRMATION.matcher(userMsg != null ? userMsg.trim() : "").matches();
        log.info("【预约工具】确认检查: userMsg='{}', regexMatch={}", userMsg, isConfirm);
        if (!isConfirm) {
            return "预约信息已整理，请核对就诊人、科室、日期、时段和医生后回复 \"确认预约\"";
        }

        String token = currentToken(memoryId);
        if (!StringUtils.hasText(token)) {
            log.warn("【预约工具】缺少登录 token，拒绝预约请求");
            return "查询号源无需登录，但正式预约需要先登录医院预约挂号平台";
        }
        log.info("【预约工具】token 已获取，准备调用远程接口，token长度={}", token.length());

        try {
            PatientReference patient = findOwnedPatient(token, appointment);
            if (patient == null) {
                log.warn("【预约工具】未找到匹配就诊人，username={}", username);
                return "当前账号中未找到匹配的就诊人，请核对姓名和证件号后四位，或先在就诊人管理中添加信息";
            }
            log.info("【预约工具】就诊人匹配成功，patientId={}, fullIdCard={}", patient.id(), patient.fullIdCard());

            Appointment appointmentDB = appointmentService.getOne(appointment);
            if (appointmentDB != null && appointmentDB.getPlatformOrderId() != null) {
                if (!Objects.equals(appointmentDB.getPatientId(), patient.id())) {
                    return "该预约记录与当前账号的就诊人不匹配，请联系管理员处理";
                }
                return "您已完成该时段的正式预约，平台订单号：" + appointmentDB.getPlatformOrderId();
            }

            if (appointmentDB == null) {
                ScheduleReference schedule = findAvailableSchedule(token, appointment);
                if (schedule == null) {
                    return "当前条件暂无可预约号源，请更换日期、时间或医生";
                }
                appointmentDB = createPendingAppointment(appointment, patient, schedule);
            } else {
                // 功能完善：失败重试沿用首次选定的排班和就诊人，保证幂等键不会漂移到另一订单。
                if (appointmentDB.getPatientId() != null
                        && !Objects.equals(appointmentDB.getPatientId(), patient.id())) {
                    return "该预约记录与当前账号的就诊人不匹配，请联系管理员处理";
                }
                boolean associationChanged = false;
                if (appointmentDB.getPatientId() == null) {
                    appointmentDB.setPatientId(patient.id());
                    associationChanged = true;
                }
                if (!StringUtils.hasText(appointmentDB.getScheduleId())) {
                    ScheduleReference schedule = findAvailableSchedule(token, appointment);
                    if (schedule == null) {
                        return "当前条件暂无可预约号源，请更换日期、时间或医生";
                    }
                    appointmentDB.setScheduleId(schedule.id());
                    appointmentDB.setStatus(STATUS_PENDING);
                    associationChanged = true;
                }
                if (associationChanged && !appointmentService.updateById(appointmentDB)) {
                    return "预约关联信息保存失败，请稍后重试";
                }
            }

            // 并发插入冲突可能返回另一线程刚创建的记录，因此提交前统一复核归属与排班关联。
            if (!Objects.equals(appointmentDB.getPatientId(), patient.id())
                    || !StringUtils.hasText(appointmentDB.getScheduleId())) {
                return "预约记录关联不完整，请稍后重试";
            }

            Long orderId = submitOfficialOrder(token, appointmentDB);
            appointmentDB.setPlatformOrderId(orderId);
            appointmentDB.setStatus(STATUS_SUBMITTED);
            if (!appointmentService.updateById(appointmentDB)) {
                // 平台订单已创建；幂等键会保证用户重试时取回同一个订单，再补写本地关联。
                log.warn("平台订单已创建但 AI 预约关联更新失败，appointmentId={}", appointmentDB.getId());
                return "正式订单已创建，订单号：" + orderId + "；本地关联稍后重试同步";
            }
            return "预约成功，已创建医院预约挂号平台正式订单，订单号：" + orderId;
        } catch (AppointmentRemoteException exception) {
            return exception.getMessage();
        } catch (DataIntegrityViolationException exception) {
            return "您在相同的科室和时间已有预约，请勿重复提交";
        } catch (RuntimeException exception) {
            log.error("AI 正式预约处理失败，memoryId={}", memoryId, exception);
            return "预约服务暂时不可用，请稍后重试";
        }
    }

    @Tool(name = "cancel_appointment", value = "取消预约挂号：取消当前登录用户的对应平台正式订单；只有平台取消成功后才删除 AI 本地预约记录。")
    public String cancelAppointment(
            @ToolMemoryId Long memoryId,
            @P(value = "就诊人姓名") String username,
            @P(value = "证件号后四位；同名就诊人需要提供，只有一个同名就诊人时可不填", required = false) String idCardLast4,
            @P(value = "科室名称") String department,
            @P(value = "日期") String date,
            @P(value = "时间，可选值：上午、下午") String time,
            @P(value = "医生名称", required = false) String doctorName) {
        Appointment appointment = appointmentFromArguments(
                username, idCardLast4, department, date, time, doctorName);
        String validationMessage = validateAppointment(appointment);
        if (validationMessage != null) {
            return validationMessage;
        }
        if (!isExplicitConfirmation(CANCEL_CONFIRMATION)) {
            return "取消信息已整理，请核对对应预约后回复“确认取消预约”";
        }
        String token = currentToken(memoryId);
        if (!StringUtils.hasText(token)) {
            return "取消预约需要先登录医院预约挂号平台";
        }

        try {
            // 完整证件号仅从当前账号的就诊人接口取得，不进入模型上下文。
            PatientReference patient = findOwnedPatient(token, appointment);
            if (patient == null) {
                return "当前账号中未找到匹配的就诊人，请核对姓名和证件号后四位";
            }
            Appointment appointmentDB = appointmentService.getOne(appointment);
            if (appointmentDB == null) {
                return "您没有对应的预约记录，请核对预约科室和时间";
            }
            if (appointmentDB.getPatientId() != null
                    && !Objects.equals(appointmentDB.getPatientId(), patient.id())) {
                return "该预约不属于当前登录账号，无法取消";
            }

            Long officialOrderId = appointmentDB.getPlatformOrderId();
            if (officialOrderId == null) {
                // 响应丢失时本地可能还没有订单号；先只查询幂等结果，禁止取消动作意外创建新订单。
                officialOrderId = findOfficialOrderByIdempotencyKey(token, appointmentDB.getId());
            }
            if (officialOrderId != null) {
                cancelOfficialOrder(token, officialOrderId);
            }
            if (appointmentService.removeById(appointmentDB.getId())) {
                return "取消预约成功";
            }
            return "平台订单已取消，但本地记录清理失败，请稍后重试";
        } catch (AppointmentRemoteException exception) {
            return exception.getMessage();
        } catch (RuntimeException exception) {
            log.error("AI 取消正式预约失败，memoryId={}", memoryId, exception);
            return "取消预约服务暂时不可用，请稍后重试";
        }
    }

    @Tool(name = "query_schedule", value = "查询是否有号源：根据科室名称、日期、时间和可选医生查询是否有号源")
    public String querySchedule(
            // 功能完善：参数名与预约领域字段统一为 department，并由 Maven 的 -parameters 配置保留到运行时。
            @P(value = "科室名称") String department,
            @P(value = "日期") String date,
            @P(value = "时间，可选值：上午、下午") String time,
            @P(value = "医生名称", required = false) String doctorName) {
        log.info("【号源查询】参数，department={}, date={}, time={}, doctorName={}",
                department, date, time, doctorName);
        if (!StringUtils.hasText(department) || !StringUtils.hasText(date) || !StringUtils.hasText(time)) {
            return "号源查询参数不完整，请补充科室、日期和上午或下午";
        }

        String url = serviceHospUrl + "/api/hosp/selectSchedule";
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url, formRequest(null, department, date, time, doctorName), Map.class);
            Map<String, Object> result = response.getBody();
            log.info("【号源查询】远程返回，url={}, httpStatus={}, result={}",
                    url, response.getStatusCode(), result);
            if (result == null || !codeEquals(result.get("code"), 200)) {
                return "号源服务返回异常，请稍后重试";
            }
            return Boolean.TRUE.equals(result.get("data"))
                    ? "有可预约号源" : "当前条件暂无可预约号源";
        } catch (RuntimeException exception) {
            log.warn("远程号源查询失败，url={}", url, exception);
            return "号源服务暂时不可用，请稍后重试";
        }
    }

    private Appointment createPendingAppointment(Appointment source,
                                                 PatientReference patient,
                                                 ScheduleReference schedule) {
        source.setId(null);
        source.setPatientId(patient.id());
        source.setScheduleId(schedule.id());
        source.setPlatformOrderId(null);
        source.setStatus(STATUS_PENDING);
        try {
            if (appointmentService.save(source)) {
                return source;
            }
        } catch (DataIntegrityViolationException exception) {
            Appointment existing = appointmentService.getOne(source);
            if (existing != null) {
                return existing;
            }
            throw exception;
        }
        throw new AppointmentRemoteException("保存预约确认记录失败，请稍后重试");
    }

    private PatientReference findOwnedPatient(String token, Appointment appointment) {
        String url = gatewayUrl + "/api/user/patient/auth/findAll";
        log.info("【远程调用】获取就诊人列表，url={}", url);
        try {
            HttpHeaders headers = authenticatedHeaders(token);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> data = requireSuccessData(response.getBody(), "获取就诊人失败");
            Object rawList = data.get("list");
            if (!(rawList instanceof List<?> patients)) {
                return null;
            }
            String expectedName = appointment.getUsername().trim();
            String expectedLast4 = normalizeLastFour(appointment.getIdCard());
            PatientReference matchedPatient = null;
            for (Object item : patients) {
                if (item instanceof Map<?, ?> patient
                        && expectedName.equals(stringValue(patient.get("name")))) {
                    String fullIdCard = normalizeIdCard(stringValue(patient.get("certificatesNo")));
                    if (StringUtils.hasText(expectedLast4) && !fullIdCard.endsWith(expectedLast4)) {
                        continue;
                    }
                    Long patientId = longValue(patient.get("id"));
                    if (patientId == null || !StringUtils.hasText(fullIdCard)) {
                        continue;
                    }
                    if (matchedPatient != null) {
                        throw new AppointmentRemoteException("当前账号存在多个同名就诊人，请补充证件号后四位后再试");
                    }
                    matchedPatient = new PatientReference(patientId, fullIdCard);
                }
            }
            if (matchedPatient != null) {
                // 数据库关联仍使用完整证件号，但该字段从受认证的用户服务取得，不由大模型传入。
                appointment.setIdCard(matchedPatient.fullIdCard());
            }
            return matchedPatient;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            throw new AppointmentRemoteException("登录状态已失效，请重新登录后再预约");
        } catch (AppointmentRemoteException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("获取当前用户就诊人失败，url={}", url, exception);
            throw new AppointmentRemoteException("暂时无法获取就诊人，请稍后重试");
        }
    }

    private ScheduleReference findAvailableSchedule(String token, Appointment appointment) {
        String url = gatewayUrl + "/api/hosp/auth/selectSchedule";
        log.info("【远程调用】查询可预约排班，url={}, department={}, date={}, time={}",
                url, appointment.getDepartment(), appointment.getDate(), appointment.getTime());
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    formRequest(token, appointment.getDepartment(), appointment.getDate(),
                            appointment.getTime(), appointment.getDoctorName()),
                    Map.class);
            Map<String, Object> data = requireSuccessData(response.getBody(), "暂无可预约号源");
            Object rawSchedule = data.get("schedule");
            if (!(rawSchedule instanceof Map<?, ?> schedule)) {
                return null;
            }
            String scheduleId = stringValue(schedule.get("scheduleId"));
            return StringUtils.hasText(scheduleId) ? new ScheduleReference(scheduleId) : null;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            throw new AppointmentRemoteException("登录状态已失效，请重新登录后再预约");
        } catch (AppointmentRemoteException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("解析正式排班失败，url={}", url, exception);
            throw new AppointmentRemoteException("号源服务暂时不可用，请稍后重试");
        }
    }

    private Long submitOfficialOrder(String token, Appointment appointment) {
        String url = gatewayUrl + "/api/order/orderInfo/auth/submitOrder/"
                + appointment.getScheduleId() + "/" + appointment.getPatientId();
        log.info("【远程调用】提交正式预约订单，url={}, scheduleId={}, patientId={}",
                url, appointment.getScheduleId(), appointment.getPatientId());
        try {
            HttpHeaders headers = authenticatedHeaders(token);
            headers.set("Idempotency-Key", "ai-appointment-" + appointment.getId());
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(headers), Map.class);
            log.info("【远程调用】submitOrder 响应: status={}, body={}", response.getStatusCode(), response.getBody());
            Map<String, Object> data = requireSuccessData(response.getBody(), "正式订单创建失败");
            Long orderId = longValue(data.get("orderId"));
            if (orderId == null) {
                throw new AppointmentRemoteException("正式订单创建失败：平台未返回订单号");
            }
            return orderId;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn("【远程调用】submitOrder 鉴权失败: {}", exception.getMessage());
            throw new AppointmentRemoteException("登录状态已失效，请重新登录后再预约");
        } catch (AppointmentRemoteException exception) {
            log.warn("【远程调用】submitOrder 业务异常: {}", exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            log.error("【远程调用】submitOrder 系统异常，appointmentId={}, class={}, msg={}",
                    appointment.getId(), exception.getClass().getName(), exception.getMessage(), exception);
            throw new AppointmentRemoteException("正式订单创建失败，请稍后重试");
        }
    }

    private void cancelOfficialOrder(String token, Long orderId) {
        String url = gatewayUrl + "/api/order/orderInfo/auth/cancelOrder/" + orderId;
        try {
            HttpHeaders headers = authenticatedHeaders(token);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> data = requireSuccessData(response.getBody(), "平台订单取消失败");
            if (!Boolean.TRUE.equals(data.get("flag"))) {
                throw new AppointmentRemoteException("平台订单取消失败，请稍后重试");
            }
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            throw new AppointmentRemoteException("登录状态已失效，请重新登录后再取消");
        } catch (AppointmentRemoteException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("取消平台正式订单失败，orderId={}", orderId, exception);
            throw new AppointmentRemoteException("平台订单取消失败，请稍后重试");
        }
    }

    private Long findOfficialOrderByIdempotencyKey(String token, Long appointmentId) {
        String url = gatewayUrl + "/api/order/orderInfo/auth/findByIdempotencyKey";
        try {
            HttpHeaders headers = authenticatedHeaders(token);
            headers.set("Idempotency-Key", "ai-appointment-" + appointmentId);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> data = requireSuccessData(response.getBody(), "查询正式订单失败");
            return longValue(data.get("orderId"));
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            throw new AppointmentRemoteException("登录状态已失效，请重新登录后再取消");
        } catch (AppointmentRemoteException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("按幂等键查询平台订单失败，appointmentId={}", appointmentId, exception);
            throw new AppointmentRemoteException("查询正式订单失败，请稍后重试");
        }
    }

    private HttpEntity<MultiValueMap<String, String>> formRequest(String token,
                                                                  String name,
                                                                  String date,
                                                                  String time,
                                                                  String doctorName) {
        HttpHeaders headers = StringUtils.hasText(token)
                ? authenticatedHeaders(token) : new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("name", name.trim());
        formData.add("date", date.trim());
        formData.add("time", time.trim());
        if (StringUtils.hasText(doctorName)) {
            formData.add("doctorName", doctorName.trim());
        }
        return new HttpEntity<>(formData, headers);
    }

    private HttpHeaders authenticatedHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requireSuccessData(Map<String, Object> result, String fallbackMessage) {
        if (result == null || !codeEquals(result.get("code"), 20000)) {
            String message = result == null ? null : stringValue(result.get("message"));
            throw new AppointmentRemoteException(StringUtils.hasText(message) ? message : fallbackMessage);
        }
        Object data = result.get("data");
        return data instanceof Map<?, ?> ? (Map<String, Object>) data : Map.of();
    }

    private String validateAppointment(Appointment appointment) {
        if (appointment == null || !StringUtils.hasText(appointment.getUsername())
                || !StringUtils.hasText(appointment.getDepartment())
                || !StringUtils.hasText(appointment.getDate())
                || !StringUtils.hasText(appointment.getTime())) {
            return "预约信息不完整，请补充就诊人姓名、科室、日期和时间";
        }
        if (StringUtils.hasText(appointment.getIdCard())) {
            String last4 = normalizeLastFour(appointment.getIdCard());
            if (last4.length() != 4 || appointment.getIdCard().trim().length() != 4) {
                return "为保护隐私，请勿发送完整证件号；如需区分同名就诊人，只提供证件号后四位";
            }
            appointment.setIdCard(last4);
        }
        return null;
    }

    private Appointment appointmentFromArguments(String username,
                                                 String idCard,
                                                 String department,
                                                 String date,
                                                 String time,
                                                 String doctorName) {
        Appointment appointment = new Appointment();
        appointment.setUsername(username);
        appointment.setIdCard(idCard);
        appointment.setDepartment(department);
        appointment.setDate(date);
        appointment.setTime(time);
        appointment.setDoctorName(doctorName);
        return appointment;
    }

    private boolean codeEquals(Object value, int expected) {
        return value instanceof Number && ((Number) value).intValue() == expected;
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private String normalizeIdCard(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : "";
    }

    private String normalizeLastFour(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : "";
    }

    private String normalizeBaseUrl(String value) {
        return value == null ? "" : value.trim().replaceAll("/+$", "");
    }

    private boolean isExplicitConfirmation(Pattern confirmationPattern) {
        String userMessage = AuthenticatedRequestContext.getUserMessage();
        return StringUtils.hasText(userMessage) && confirmationPattern.matcher(userMessage.trim()).matches();
    }

    private record PatientReference(Long id, String fullIdCard) {
    }

    private record ScheduleReference(String id) {
    }

    private static final class AppointmentRemoteException extends RuntimeException {
        private AppointmentRemoteException(String message) {
            super(message);
        }
    }
}
