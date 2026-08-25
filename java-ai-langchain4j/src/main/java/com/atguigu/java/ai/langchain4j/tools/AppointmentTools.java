package com.atguigu.java.ai.langchain4j.tools;

import com.atguigu.java.ai.langchain4j.entity.Appointment;
import com.atguigu.java.ai.langchain4j.service.AppointmentService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
public class AppointmentTools {

    private final AppointmentService appointmentService;
    private final RestTemplate restTemplate;
    private final String serviceHospUrl;

    @Autowired
    public AppointmentTools(AppointmentService appointmentService,
                            RestTemplateBuilder restTemplateBuilder,
                            @Value("${yygh.service-hosp-url:http://localhost:8201}") String serviceHospUrl) {
        this.appointmentService = appointmentService;
        // 功能完善：远程号源查询设置连接和读取超时，避免 AI 对话线程无限等待。
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.serviceHospUrl = serviceHospUrl.replaceAll("/+$", "");
    }

    @Tool(name="预约挂号", value = "根据参数，先执行工具方法querySchedule查询是否可预约，并直接给用户回答是否可预约，并让用户确认所有预约信息，用户确认后再进行预约。")
    public String bookAppointment(Appointment appointment){

        if (appointment == null || !StringUtils.hasText(appointment.getUsername())
                || !StringUtils.hasText(appointment.getIdCard())
                || !StringUtils.hasText(appointment.getDepartment())
                || !StringUtils.hasText(appointment.getDate())
                || !StringUtils.hasText(appointment.getTime())) {
            return "预约信息不完整，请补充姓名、身份证号、科室、日期和时间";
        }

        // 功能完善：即使模型未按提示先调用查询工具，预约方法本身也必须再次确认号源。
        if (!querySchedule(appointment.getDepartment(), appointment.getDate(),
                appointment.getTime(), appointment.getDoctorName())) {
            return "当前条件暂无可预约号源，请更换日期、时间或医生";
        }

        //查找数据库中是否包含对应的预约记录
        Appointment appointmentDB = appointmentService.getOne(appointment);
        if(appointmentDB == null){
            appointment.setId(null);//防止大模型幻觉设置了id
            try {
                if(appointmentService.save(appointment)){
                    return "预约成功，并返回预约详情";
                }
            } catch (DataIntegrityViolationException exception) {
                // 数据库唯一约束负责兜底并发重复预约。
                return "您在相同的科室和时间已有预约";
            }
            return "预约失败";
        }
        return "您在相同的科室和时间已有预约";
    }

    @Tool(name="取消预约挂号", value = "根据参数，查询预约是否存在，如果存在则删除预约记录并返回取消预约成功，否则返回取消预约失败")
    public String cancelAppointment(Appointment appointment){
        Appointment appointmentDB = appointmentService.getOne(appointment);
        if(appointmentDB != null){
            //删除预约记录
            if(appointmentService.removeById(appointmentDB.getId())){
                return "取消预约成功";
            }else{
                return "取消预约失败";
            }
        }
        //取消失败
        return "您没有预约记录，请核对预约科室和时间";
    }

    @Tool(name = "查询是否有号源", value="根据科室名称，日期，时间和医生查询是否有号源，并返回给用户")
    public boolean querySchedule(
            @P(value = "科室名称") String name,
            @P(value = "日期") String date,
            @P(value = "时间，可选值：上午、下午") String time,
            @P(value = "医生名称", required = false) String doctorName
    ) {
        if (!StringUtils.hasText(name) || !StringUtils.hasText(date) || !StringUtils.hasText(time)) {
            log.warn("号源查询缺少必填参数: name={}, date={}, time={}", name, date, time);
            return false;
        }

        String url = serviceHospUrl + "/api/hosp/selectSchedule";

        try {
            // 设置请求头为表单提交格式
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // 封装表单参数
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("name", name);
            formData.add("date", date);
            formData.add("time", time);
            if (StringUtils.hasText(doctorName)) {
                formData.add("doctorName", doctorName);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

            // 发送POST请求
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> result = response.getBody();

            // 功能完善：只接受平台明确返回的成功状态，异常或无号均按不可预约处理。
            return result != null && Integer.valueOf(200).equals(result.get("code"));
        } catch (Exception e) {
            log.warn("远程号源查询失败，url={}", url, e);
        }

        return false;
    }
}
