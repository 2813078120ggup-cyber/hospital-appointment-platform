package com.atguigu.hospital.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.hospital.mapper.DoctorMapper;
import com.atguigu.hospital.mapper.HospitalSetMapper;
import com.atguigu.hospital.mapper.ScheduleMapper;
import com.atguigu.hospital.model.Doctor;
import com.atguigu.hospital.model.HospitalSet;
import com.atguigu.hospital.model.Schedule;
import com.atguigu.hospital.service.ApiService;
import com.atguigu.hospital.util.Result;
import com.atguigu.hospital.util.YyghException;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "医院管理接口")
@Controller
@RequestMapping
public class ApiController extends BaseController {

    @Autowired
    private ApiService apiService;

    @Autowired
    private HospitalSetMapper hospitalSetMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    //医院设置回显
    @RequestMapping("/hospitalSet/index")
    public String getHospitalSet(ModelMap model, RedirectAttributes redirectAttributes) {
        HospitalSet hospitalSet = hospitalSetMapper.selectById(1);
        model.addAttribute("hospitalSet", hospitalSet);
        return "hospitalSet/index";
    }

    //医院设置保存
    @RequestMapping(value = "/hospitalSet/save")
    public String createHospitalSet(ModelMap model, HospitalSet hospitalSet) {
        hospitalSetMapper.updateById(hospitalSet);
        return "redirect:/hospitalSet/index";
    }

    //医院管理主页面
    @RequestMapping("/hospital/index")
    public String getHospital(ModelMap model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            HospitalSet hospitalSet = hospitalSetMapper.selectById(1);
            if (null == hospitalSet || StringUtils.isEmpty(hospitalSet.getHoscode()) || StringUtils.isEmpty(hospitalSet.getSignKey())) {
                this.failureMessage("先设置医院code与签名key", redirectAttributes);
                return "redirect:/hospitalSet/index";
            }
            model.addAttribute("hospital", apiService.getHospital());
            //model.addAttribute("total", 1);
            //model.addAttribute("pageNum", 1);
        } catch (YyghException e) {
            this.failureMessage(e.getMessage(), request);
        } catch (Exception e) {
            this.failureMessage("数据异常", request);
        }
        return "hospital/index";
    }

    //跳转医院添加页面
    @RequestMapping(value = "/hospital/create")
    public String createHospital(ModelMap model) {
        try {
            model.addAttribute("hospital", apiService.getHospital());
        } catch (Exception exception) {
            model.addAttribute("hospital", null);
        }
        return "hospital/create";
    }

    //医院信息保存   data为json数据
    @RequestMapping(value = "/hospital/save", method = RequestMethod.POST)
    public String saveHospital(@RequestParam(required = false) String data, HttpServletRequest request) {
        try {
            apiService.saveHospital(StringUtils.hasText(data) ? data : buildHospitalData(request).toJSONString());
        } catch (YyghException e) {
            return this.failurePage(e.getMessage(), request);
        } catch (Exception e) {
            return this.failurePage("数据异常", request);
        }
        return this.successPage(null, request);
    }

    @RequestMapping("/department/list")
    public String findDepartment(ModelMap model,
                                 @RequestParam(defaultValue = "1") int pageNum,
                                 @RequestParam(defaultValue = "10") int pageSize,
                                 @RequestParam(required = false) String depcode,
                                 HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            HospitalSet hospitalSet = hospitalSetMapper.selectById(1);
            if (null == hospitalSet || StringUtils.isEmpty(hospitalSet.getHoscode()) || StringUtils.isEmpty(hospitalSet.getSignKey())) {
                this.failureMessage("先设置医院code与签名key", redirectAttributes);
                return "redirect:/hospitalSet/index";
            }

            model.addAllAttributes(apiService.findDepartment(pageNum, pageSize, depcode));
            model.addAttribute("depcode", depcode);
            model.addAttribute("pageSize", pageSize);
        } catch (YyghException e) {
            this.failureMessage(e.getMessage(), request);
        } catch (Exception e) {
            this.failureMessage("数据异常", request);
        }
        return "department/index";
    }

    @RequestMapping(value = "/department/create")
    public String create(ModelMap model) {
        model.addAttribute("department", new HashMap<String, Object>());
        return "department/create";
    }

    @RequestMapping(value = "/department/edit/{depcode}")
    public String editDepartment(ModelMap model, @PathVariable String depcode) {
        Map<String, Object> result = apiService.findDepartment(1, 1, depcode);
        Object list = result.get("list");
        JSONObject department = list instanceof JSONArray && !((JSONArray) list).isEmpty()
                ? ((JSONArray) list).getJSONObject(0) : new JSONObject();
        model.addAttribute("department", new HashMap<String, Object>(department));
        return "department/create";
    }

    @RequestMapping(value = "/department/save", method = RequestMethod.POST)
    public String save(@RequestParam(required = false) String data, HttpServletRequest request) {
        try {
            apiService.saveDepartment(StringUtils.hasText(data) ? data : buildDepartmentData(request).toJSONString());
        } catch (YyghException e) {
            return this.failurePage(e.getMessage(), request);
        } catch (Exception e) {
            return this.failurePage("数据异常", request);
        }
        return this.successPage(null, request);
    }

    @RequestMapping("/schedule/list")
    public String findSchedule(ModelMap model,
                               @RequestParam(defaultValue = "1") int pageNum,
                               @RequestParam(defaultValue = "10") int pageSize,
                               @RequestParam(required = false) String depcode,
                               @RequestParam(required = false) String doctorName,
                               @RequestParam(required = false) String workDate,
                               @RequestParam(required = false) Integer status,
                               HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            HospitalSet hospitalSet = hospitalSetMapper.selectById(1);
            if (null == hospitalSet || StringUtils.isEmpty(hospitalSet.getHoscode()) || StringUtils.isEmpty(hospitalSet.getSignKey())) {
                this.failureMessage("先设置医院code与签名key", redirectAttributes);
                return "redirect:/hospitalSet/index";
            }

            model.addAllAttributes(apiService.findSchedule(pageNum, pageSize, depcode,
                    doctorName, workDate, status, null));
            model.addAttribute("depcode", depcode);
            model.addAttribute("doctorName", doctorName);
            model.addAttribute("workDate", workDate);
            model.addAttribute("status", status);
            model.addAttribute("pageSize", pageSize);
        } catch (YyghException e) {
            this.failureMessage(e.getMessage(), request);
        } catch (Exception e) {
            this.failureMessage("数据异常", request);
        }
        return "schedule/index";
    }

    @RequestMapping(value = "/schedule/create")
    public String createSchedule(ModelMap model) {
        model.addAttribute("schedule", new HashMap<String, Object>());
        model.addAttribute("doctors", enabledDoctors());
        return "schedule/create";
    }

    @RequestMapping(value = "/schedule/edit/{hosScheduleId}")
    public String editSchedule(ModelMap model, @PathVariable String hosScheduleId) {
        Map<String, Object> result = apiService.findSchedule(1, 1, null,
                null, null, null, hosScheduleId);
        Object list = result.get("list");
        JSONObject schedule = list instanceof JSONArray && !((JSONArray) list).isEmpty()
                ? ((JSONArray) list).getJSONObject(0) : new JSONObject();
        model.addAttribute("schedule", new HashMap<String, Object>(schedule));
        model.addAttribute("doctors", enabledDoctors());
        return "schedule/create";
    }

    @RequestMapping(value = "/schedule/save", method = RequestMethod.POST)
    public String saveSchedule(@RequestParam(required = false) String data, HttpServletRequest request) {
        try {
            apiService.saveSchedule(StringUtils.hasText(data) ? data : buildScheduleData(request).toJSONString());
        } catch (YyghException e) {
            return this.failurePage(e.getMessage(), request);
        } catch (Exception e) {
            e.printStackTrace();
            return this.failurePage("数据异常：" + e.getMessage(), request);
        }
        return this.successPage(null, request);
    }

    //医院管理 - 批量导入测试数据
    @RequestMapping(value = "/hospital/createBatch")
    public String createHospitalBatch(ModelMap model) {
        return "hospital/createBatch";
    }

    //医院管理 - 批量保存 classpath:hospital.json
    @RequestMapping(value = "/hospital/saveBatch", method = RequestMethod.POST)
    public String saveBatchHospital(HttpServletRequest request) {
        try {
            apiService.saveBatchHospital();
        } catch (YyghException e) {
            return this.failurePage(e.getMessage(), request);
        } catch (Exception e) {
            return this.failurePage("数据异常", request);
        }
        return this.successPage(null, request);
    }

    @RequestMapping(value = "/department/remove/{depcode}", method = RequestMethod.POST)
    @ResponseBody
    public Result removeDepartment(@PathVariable String depcode) {
        try {
            apiService.removeDepartment(depcode);
            return Result.ok();
        } catch (YyghException exception) {
            return Result.fail().message(exception.getMessage());
        }
    }

    @RequestMapping(value = "/schedule/remove/{hosScheduleId}", method = RequestMethod.POST)
    @ResponseBody
    public Result removeSchedule(@PathVariable String hosScheduleId) {
        try {
            apiService.removeSchedule(hosScheduleId);
            return Result.ok();
        } catch (YyghException exception) {
            return Result.fail().message(exception.getMessage());
        }
    }

    @RequestMapping(value = "/schedule/status", method = RequestMethod.POST)
    @ResponseBody
    public Result updateScheduleStatus(@RequestParam Long scheduleId, @RequestParam Integer status) {
        try {
            if (status == null || (status != -1 && status != 0 && status != 1)) {
                throw new YyghException("排班状态不正确", 201);
            }
            Schedule schedule = scheduleMapper.selectById(scheduleId);
            if (schedule == null) {
                throw new YyghException("排班不存在", 201);
            }
            apiService.suspendSchedule(String.valueOf(scheduleId), status);
            schedule.setStatus(status);
            scheduleMapper.updateById(schedule);
            return Result.ok();
        } catch (YyghException exception) {
            return Result.fail().message(exception.getMessage());
        }
    }

    //意见反馈列表
    @RequestMapping("/feedback/list")
    public String findFeedback(ModelMap model,
                               @RequestParam(defaultValue = "1") int pageNum,
                               @RequestParam(defaultValue = "10") int pageSize,
                               HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            HospitalSet hospitalSet = hospitalSetMapper.selectById(1);
            if (null == hospitalSet || StringUtils.isEmpty(hospitalSet.getHoscode()) || StringUtils.isEmpty(hospitalSet.getSignKey())) {
                this.failureMessage("先设置医院code与签名key", redirectAttributes);
                return "redirect:/hospitalSet/index";
            }

            model.addAllAttributes(apiService.findFeedback(pageNum, pageSize));
        } catch (YyghException e) {
            this.failureMessage(e.getMessage(), request);
        } catch (Exception e) {
            this.failureMessage("数据异常", request);
        }
        return "feedback/index";
    }

    //意见反馈处理
    @RequestMapping(value = "/feedback/handle", method = RequestMethod.POST)
    public String handleFeedback(@RequestParam Long id,
                                 @RequestParam Integer status,
                                 @RequestParam(required = false) String reply,
                                 RedirectAttributes redirectAttributes) {
        try {
            apiService.handleFeedback(id, status, reply);
            this.successMessage("反馈处理成功", redirectAttributes);
        } catch (YyghException e) {
            this.failureMessage(e.getMessage(), redirectAttributes);
        } catch (Exception e) {
            this.failureMessage("数据异常", redirectAttributes);
        }
        return "redirect:/feedback/list";
    }

    private JSONObject buildHospitalData(HttpServletRequest request) {
        JSONObject hospital = new JSONObject();
        copyParameter(request, hospital, "hosname");
        copyParameter(request, hospital, "hostype");
        copyParameter(request, hospital, "provinceCode");
        copyParameter(request, hospital, "cityCode");
        copyParameter(request, hospital, "districtCode");
        copyParameter(request, hospital, "address");
        copyParameter(request, hospital, "intro");
        copyParameter(request, hospital, "route");
        String logoData = request.getParameter("logoData");
        if (!StringUtils.hasText(logoData)) {
            try {
                JSONObject existing = apiService.getHospital();
                logoData = existing == null ? null : existing.getString("logoData");
            } catch (Exception ignored) {
                logoData = null;
            }
        }
        hospital.put("logoData", logoData);

        JSONObject rule = new JSONObject();
        rule.put("cycle", parseInteger(request.getParameter("cycle"), 7));
        rule.put("releaseTime", request.getParameter("releaseTime"));
        rule.put("stopTime", request.getParameter("stopTime"));
        rule.put("quitDay", parseInteger(request.getParameter("quitDay"), -1));
        rule.put("quitTime", request.getParameter("quitTime"));
        String ruleText = request.getParameter("rule");
        rule.put("rule", Collections.singletonList(ruleText == null ? "" : ruleText.trim()));
        hospital.put("bookingRule", rule);
        return hospital;
    }

    private JSONObject buildDepartmentData(HttpServletRequest request) {
        JSONObject department = new JSONObject();
        copyParameter(request, department, "depcode");
        copyParameter(request, department, "depname");
        copyParameter(request, department, "bigcode");
        copyParameter(request, department, "bigname");
        copyParameter(request, department, "intro");
        if (!StringUtils.hasText(department.getString("depcode"))
                || !StringUtils.hasText(department.getString("depname"))) {
            throw new YyghException("请填写科室编号和名称", 201);
        }
        return department;
    }

    private JSONObject buildScheduleData(HttpServletRequest request) {
        JSONObject schedule = new JSONObject();
        copyParameter(request, schedule, "hosScheduleId");
        copyParameter(request, schedule, "depcode");
        copyParameter(request, schedule, "title");
        copyParameter(request, schedule, "docname");
        copyParameter(request, schedule, "skill");
        copyParameter(request, schedule, "workDate");
        copyParameter(request, schedule, "amount");
        schedule.put("workTime", parseInteger(request.getParameter("workTime"), 0));
        schedule.put("reservedNumber", parseInteger(request.getParameter("reservedNumber"), 0));
        schedule.put("availableNumber", parseInteger(request.getParameter("availableNumber"), 0));
        schedule.put("status", parseInteger(request.getParameter("status"), 1));
        if (!StringUtils.hasText(schedule.getString("hosScheduleId"))
                || !StringUtils.hasText(schedule.getString("depcode"))
                || !StringUtils.hasText(schedule.getString("docname"))
                || !StringUtils.hasText(schedule.getString("workDate"))) {
            throw new YyghException("请完整填写排班编号、科室、医生和日期", 201);
        }
        if (schedule.getInteger("reservedNumber") < 0
                || schedule.getInteger("availableNumber") < 0
                || schedule.getInteger("availableNumber") > schedule.getInteger("reservedNumber")) {
            throw new YyghException("剩余号源应为 0 到总号源之间", 201);
        }
        try {
            Long.parseLong(schedule.getString("hosScheduleId"));
        } catch (NumberFormatException exception) {
            throw new YyghException("院内排班编号必须是数字", 201);
        }
        return schedule;
    }

    private void copyParameter(HttpServletRequest request, JSONObject target, String name) {
        String value = request.getParameter(name);
        target.put(name, value == null ? null : value.trim());
    }

    private int parseInteger(String value, int defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new YyghException("数值格式不正确", 201);
        }
    }

    private List<Doctor> enabledDoctors() {
        return doctorMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Doctor>()
                .eq("status", 1).orderByAsc("docname"));
    }

}
