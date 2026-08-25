package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.hosp.repository.ScheduleRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.BookingRule;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.BookingScheduleRuleVo;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private DepartmentService departmentService;

    //下单辅助方法：根据排班主键查询排班订单信息
    // 根据排班id查询预约挂号相关数据
    @Override
    public ScheduleOrderVo getScheduleOrderVo(String scheduleId) {

        //封装返回结果数据
        ScheduleOrderVo orderVo = new ScheduleOrderVo();

        //根据排班id获取排班信息
        // 1. 根据排班id查询mongo排班实体对象
        Schedule schedule = this.getScheduleId(scheduleId);
        // if排班对象为null抛出自定义异常
        if(schedule == null){
            throw new YyghException(20001, "排班不存在");
        }

        //根据医院编号获取医院信息
        Hospital hospital = hospitalService.getHosp(schedule.getHoscode()); //MongoDB
        if(hospital == null){
            throw new YyghException(20001, "排班所属医院不存在");
        }

        //获取挂号规则
        BookingRule bookingRule = hospital.getBookingRule();

        orderVo.setHoscode(hospital.getHoscode());
        orderVo.setHosname(hospital.getHosname());

        orderVo.setDepcode(schedule.getDepcode());
        Department department = departmentService.getDepartment(hospital.getHoscode(), schedule.getDepcode()); //MongoDB
        if (department == null) {
            throw new YyghException(20001, "排班所属科室不存在");
        }
        orderVo.setDepname(department.getDepname());

        orderVo.setHosScheduleId(schedule.getHosScheduleId()); //排班编号
        orderVo.setAvailableNumber(schedule.getAvailableNumber()); //剩余预约数量
        orderVo.setTitle(schedule.getTitle()); //医生职称
        orderVo.setReserveDate(schedule.getWorkDate()); //排班日期
        orderVo.setReserveTime(schedule.getWorkTime()); //排班时间  0-上午   1-下午
        orderVo.setAmount(schedule.getAmount()); //医事服务费

        //退号截止天数 -1为就诊前一天  -为当天
        Integer quitDayCount = bookingRule.getQuitDay();
        Date quitDay = new DateTime(schedule.getWorkDate()).plusDays(quitDayCount).toDate();
        DateTime quitTime = getDateTime(quitDay, bookingRule.getQuitTime());
        orderVo.setQuitTime(quitTime.toDate()); //退号时间

        DateTime startTime = this.getDateTime(new Date(), bookingRule.getReleaseTime());
        DateTime endTime = this.getDateTime(new DateTime().plusDays(bookingRule.getCycle()).toDate(), bookingRule.getStopTime());
        orderVo.setStartTime(startTime.toDate());//预约开始时间
        orderVo.setEndTime(endTime.toDate()); //预约结束时间

        DateTime stopTime = getDateTime(new Date(), bookingRule.getStopTime());
        orderVo.setStopTime(stopTime.toDate()); //当前停止挂号时间

        return orderVo;
    }


    @Override
    public void update(Schedule schedule) {
        scheduleRepository.save(schedule);  // 由_id决定(save方法内部逻辑): 如果_id存在则更新，不存在则插入
    }

    @Override
    public boolean hasAvailableSchedule(String departmentName,
                                        String date,
                                        String time,
                                        String doctorName) {
        return findAvailableSchedule(departmentName, date, time, doctorName) != null;
    }

    @Override
    public Schedule findAvailableSchedule(String departmentName,
                                           String date,
                                           String time,
                                           String doctorName) {
        if (departmentName == null || departmentName.trim().isEmpty()) {
            throw new IllegalArgumentException("科室名称不能为空");
        }

        Date workDate = parseWorkDate(date);
        Integer workTime = parseWorkTime(time);
        String normalizedDoctorName = doctorName == null ? null : doctorName.trim();
        List<Department> departments = departmentService.findByDepname(departmentName.trim());

        if (departments == null || departments.isEmpty()) {
            return null;
        }

        Schedule selectedSchedule = null;
        for (Department department : departments) {
            Schedule candidate;
            if (normalizedDoctorName == null || normalizedDoctorName.isEmpty()) {
                candidate = scheduleRepository
                        .findFirstByHoscodeAndDepcodeAndWorkDateAndWorkTimeAndStatusAndAvailableNumberGreaterThanOrderByAvailableNumberDesc(
                                department.getHoscode(),
                                department.getDepcode(),
                                workDate,
                                workTime,
                                1,
                                0);
            } else {
                candidate = scheduleRepository
                        .findFirstByHoscodeAndDepcodeAndWorkDateAndWorkTimeAndDocnameAndStatusAndAvailableNumberGreaterThanOrderByAvailableNumberDesc(
                                department.getHoscode(),
                                department.getDepcode(),
                                workDate,
                                workTime,
                                normalizedDoctorName,
                                1,
                                0);
            }
            if (candidate != null && (selectedSchedule == null
                    || candidate.getAvailableNumber() > selectedSchedule.getAvailableNumber())) {
                selectedSchedule = candidate;
            }
        }
        // 对多院区同名科室选择剩余号源更多的排班，并补齐前端及 AI 所需的医院、科室名称。
        return selectedSchedule == null ? null : packageSchedule(selectedSchedule);
    }

    private Date parseWorkDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("日期不能为空");
        }
        try {
            return DateTimeFormat.forPattern("yyyy-MM-dd")
                    .parseDateTime(date.trim())
                    .withTimeAtStartOfDay()
                    .toDate();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("日期格式应为yyyy-MM-dd", exception);
        }
    }

    private Integer parseWorkTime(String time) {
        if (time == null || time.trim().isEmpty()) {
            throw new IllegalArgumentException("时间不能为空");
        }
        return switch (time.trim()) {
            case "上午", "0" -> 0;
            case "下午", "1" -> 1;
            default -> throw new IllegalArgumentException("时间仅支持上午或下午");
        };
    }









    //上传排班
    @Override
    public void saveSchedule(Map<String, Object> newObjectMap) {
        String jsonString = JSONObject.toJSONString(newObjectMap);
        Schedule schedule = JSONObject.parseObject(jsonString, Schedule.class);

        //  //根据医院编号 和 排班编号查询
        Schedule existSchedule = scheduleRepository.getScheduleByHoscodeAndHosScheduleId(schedule.getHoscode(),
                schedule.getHosScheduleId());
        if (existSchedule != null) {
            schedule.setId(existSchedule.getId());
            schedule.setCreateTime(existSchedule.getCreateTime());
            schedule.setUpdateTime(new Date());
            scheduleRepository.save(schedule);
        } else {
            schedule.setCreateTime(new Date());
            schedule.setUpdateTime(new Date());
            scheduleRepository.save(schedule);
        }
    }

    @Override
    public Page<Schedule> selectPageSchedule(int page, int limit, String hoscode, String depcode) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        //0为第一页
        Pageable pageable = PageRequest.of(page - 1, limit, sort);
        //封装条件
        Schedule schedule = new Schedule();
        schedule.setHoscode(hoscode);
        schedule.setDepcode(depcode);
        Example<Schedule> example = Example.of(schedule);

        // 功能完善：分页查询必须应用医院和科室条件，避免跨医院返回排班。
        Page<Schedule> all = scheduleRepository.findAll(example, pageable);
        return all;
    }

    //删除
    @Override
    public void remove(String hoscode, String hosScheduleId) {
        Schedule schedule = scheduleRepository.getScheduleByHoscodeAndHosScheduleId(hoscode, hosScheduleId);
        if (null != schedule) {
            scheduleRepository.deleteById(schedule.getId());
        }
    }

    //MongoTemplate聚合操作
    //根据医院编号 + 科室编号，查询可以预约日期数据，分页显示
    @Override
    public Map<String, Object> findScheduleRule(long page,
                                                long limit,
                                                String hoscode,
                                                String depcode) {
        //封装条件 医院编号 + 科室编号
        Criteria criteria = Criteria.where("hoscode").is(hoscode)
                .and("depcode").is(depcode);
        //封装聚合条件
        Aggregation aggregation = Aggregation.newAggregation(
                //条件匹配 根据医院编号 + 科室编号
                Aggregation.match(criteria),

                //分组 workDate
                //SELECT workDate AS workDate FROM users GROUP BY workDate
                Aggregation.group("workDate")
                        .first("workDate").as("workDate")
                        // 分组基础之上 统计数量
                        .count().as("docCount")
                        // 分组基础之上 求和号数量
                        .sum("reservedNumber").as("reservedNumber")
                        .sum("availableNumber").as("availableNumber"),

                // 设置排序
                Aggregation.sort(Sort.Direction.ASC, "workDate"),

                // 设置分页条件
                //开始位置 ： (当前页-1)*每页记录数
                Aggregation.skip((page - 1) * limit),
                Aggregation.limit(limit)
        );

        //调用mongoTemplate聚合查询
        AggregationResults<BookingScheduleRuleVo> aggregateResults =
                mongoTemplate.aggregate(aggregation,
                        Schedule.class,
                        BookingScheduleRuleVo.class);
        //查询得到数据集合
        List<BookingScheduleRuleVo> bookingScheduleRuleVoList = aggregateResults.getMappedResults();

        //遍历bookingScheduleRuleVoList集合
        for (BookingScheduleRuleVo bookingScheduleRuleVo : bookingScheduleRuleVoList) {
            //获取每个对象日期
            Date workDate = bookingScheduleRuleVo.getWorkDate();
            //调用工具方法，根据日期返回对应星期
            //Date -- DateTime    new DateTime(workDate)
            String dayOfWeek = this.getDayOfWeek(new DateTime(workDate));
            //返回星期封装到对象里面
            bookingScheduleRuleVo.setDayOfWeek(dayOfWeek);
        }

        //查询总记录数
        //聚合查询，去掉分页条件，得到list集合，list集合长度
        Aggregation totalAggregation = Aggregation.newAggregation(
                //条件匹配 根据医院编号 + 科室编号
                Aggregation.match(criteria),
                //分组 workDate
                //SELECT workDate AS workDate FROM users GROUP BY workDate
                Aggregation.group("workDate")
        );
        AggregationResults<BookingScheduleRuleVo> totalAggregate = mongoTemplate.aggregate(totalAggregation, Schedule.class, BookingScheduleRuleVo.class);
        List<BookingScheduleRuleVo> mappedResultsList = totalAggregate.getMappedResults();
        int total = mappedResultsList.size();

        //封装数据到map，返回
        Map<String, Object> result = new HashMap<>();
        result.put("bookingScheduleRuleList", bookingScheduleRuleVoList);
        result.put("total", total);
        //获取医院名称
        Hospital hosp = hospitalService.getHosp(hoscode);
        //其他基础数据
        Map<String, String> baseMap = new HashMap<>();
        baseMap.put("hosname", hosp.getHosname());
        result.put("baseMap", baseMap);

        return result;
    }

    //根据医院编号 + 科室编号 + 工作日期，查询科室里面医生排班详细信息
    @Override
    public List<Schedule> getScheduleDataDetail(String hoscode,
                                                String depcode,
                                                String workDate) {
        //因为workDate在mongoDB是Date类型，转换Date类型
        // String -- Date
        Date date = new DateTime(workDate).toDate();
        List<Schedule> list =
                scheduleRepository.getScheduleByHoscodeAndDepcodeAndWorkDate(hoscode,
                        depcode, date);
        return list;
    }

    //显示科室可以预约日期数据
    // 医院编号 +  科室编号  + 分页参数
    @Override
    public Map<String, Object> getBookingScheduleRule(Integer page,
                                                      Integer limit,
                                                      String hoscode,
                                                      String depcode) {
        //1 获取所有显示日期  根据当前日期 + 预约周期
        //根据医院编号获取预约信息
        Hospital hospital = hospitalService.getHosp(hoscode);
        //获取预约规则
        BookingRule bookingRule = hospital.getBookingRule();
        //根据当前日期 + 预约周期获取每页显示日期数据
        IPage iPage = this.getListDate(page, limit, bookingRule);
        List<Date> dateList = iPage.getRecords();

        //2 根据医院编号 +  科室编号  + 所有日期进行查询
        //MongoTemplate聚合查询
        //根据workDate进行分组
        //封装查询条件
        Criteria criteria = Criteria.where("hoscode").is(hoscode)
                .and("depcode").is(depcode)
                .and("workDate").in(dateList);
        //封装聚合条件
        Aggregation agg = Aggregation.newAggregation(
                //匹配条件
                Aggregation.match(criteria),
                //分组 workDate
                Aggregation.group("workDate")
                        .first("workDate").as("workDate")
                        //分组基础之上 统计 求和
                        .count().as("docCount")
                        .sum("availableNumber").as("availableNumber")
                        .sum("reservedNumber").as("reservedNumber")
        );
        //调用mongoTemplate方法
        AggregationResults<BookingScheduleRuleVo> aggregateResult
                = mongoTemplate.aggregate(agg, Schedule.class, BookingScheduleRuleVo.class);
        List<BookingScheduleRuleVo> scheduleRuleVoList = aggregateResult.getMappedResults();

        //3 查看所有日期，每个日期是否有号
        //判断显示每个日期 在mongodb查询数据中是否存在，如果存在设置有号 不存在设置无号
        // scheduleRuleVoList-- map<日期,日期对应数据>
        Map<Date, BookingScheduleRuleVo> scheduleRuleVoMap =
                scheduleRuleVoList.stream()
                        .collect(Collectors.toMap(BookingScheduleRuleVo::getWorkDate,
                                BookingScheduleRuleVo -> BookingScheduleRuleVo));

        //根据日期查询map的key
        List<BookingScheduleRuleVo> bookingScheduleRuleVoList = new ArrayList<>();
        //dateList每页所有日期，遍历，得到每个日期
        int len = dateList.size();
        for (int i = 0; i < len; i++) {
            Date date = dateList.get(i);
            //拿着每个日期到map查询，map的key就是日期
            BookingScheduleRuleVo bookingScheduleRuleVo = scheduleRuleVoMap.get(date);
            if (bookingScheduleRuleVo == null) { //无号
                bookingScheduleRuleVo = new BookingScheduleRuleVo();
                //就诊医生人数
                bookingScheduleRuleVo.setDocCount(0);
                //科室剩余预约数  -1表示无号
                bookingScheduleRuleVo.setAvailableNumber(-1);
            }
            //有号
            bookingScheduleRuleVo.setWorkDate(date);
            bookingScheduleRuleVo.setWorkDateMd(date);
            //计算当前预约日期为周几
            String dayOfWeek = this.getDayOfWeek(new DateTime(date));
            bookingScheduleRuleVo.setDayOfWeek(dayOfWeek);

            //最后一个日期，显示即将放号   状态  0：正常  1：即将放号  -1：当天已停止挂号
            //最后一页的最后一条记录
            if (i == len - 1 && page == iPage.getPages()) {
                bookingScheduleRuleVo.setStatus(1);
            } else {
                bookingScheduleRuleVo.setStatus(0);
            }

            // 第一个日期，如果当前日期时间过了停止挂号时候，显示 停止挂号 -1、
            if (i == 0 && page == 1) {
                DateTime stopTime = this.getDateTime(new Date(), bookingRule.getStopTime());
                if (stopTime.isBeforeNow()) {
                    bookingScheduleRuleVo.setStatus(-1);
                }
            }

            //放到最终list集合
            bookingScheduleRuleVoList.add(bookingScheduleRuleVo);
        }

        //封装map集合返回
        Map<String, Object> result = new HashMap<>();
        //可预约日期规则数据
        result.put("bookingScheduleList", bookingScheduleRuleVoList);
        result.put("total", iPage.getTotal());
        //其他基础数据
        Map<String, String> baseMap = new HashMap<>();
        //医院名称
        baseMap.put("hosname", hospitalService.getHosp(hoscode).getHosname());
        //科室
        Department department = departmentService.getDepartment(hoscode, depcode);
        //大科室名称
        baseMap.put("bigname", department.getBigname());
        //科室名称
        baseMap.put("depname", department.getDepname());
        //月
        baseMap.put("workDateString", new DateTime().toString("yyyy年MM月"));
        //放号时间
        baseMap.put("releaseTime", bookingRule.getReleaseTime());
        //停号时间
        baseMap.put("stopTime", bookingRule.getStopTime());
        result.put("baseMap", baseMap);
        return result;
    }

    //获取排班详情
    @Override
    public Schedule getScheduleId(String id) {
        return scheduleRepository.findById(id)
                .map(this::packageSchedule)
                .orElse(null);
    }


    //封装医院名称和科室名称
    private Schedule packageSchedule(Schedule schedule) {
        String hoscode = schedule.getHoscode();
        String depcode = schedule.getDepcode();
        //医院名称
        Hospital hosp = hospitalService.getHosp(hoscode);
        schedule.getParam().put("hosname", hosp.getHosname());
        //科室名称
        Department department = departmentService.getDepartment(hoscode, depcode);
        schedule.getParam().put("depname", department.getDepname());

        return schedule;
    }

    //根据当前日期 + 预约周期  获取所有显示日期（分页）
    private IPage getListDate(Integer page, Integer limit, BookingRule bookingRule) {
        //逻辑处理: 判断当前日期时间是否过了放号时间，如果过了放号时候，预约周期+1
        // releaseTime  08:30
        //放号日期时间
        // 2022-11-03 08:30
        DateTime releaseTime = this.getDateTime(new Date(), bookingRule.getReleaseTime());
        //预约周期
        Integer cycle = bookingRule.getCycle();
        // 判断当前日期时间是否过了放号时间，如果过了放号时候，预约周期+1
        if (releaseTime.isBeforeNow()) {
            cycle += 1;
        }

        //获取显示所有日期   根据当前日期 + 预约周期
        List<Date> dateList = new ArrayList<>();
        for (int i = 0; i < cycle; i++) {
            //让当前日期 + 操作
            DateTime currentDateTime = new DateTime().plusDays(i);
            // DateTime -- Date类型
            String dateString = currentDateTime.toString("yyyy-MM-dd");
            Date date = new DateTime(dateString).toDate();
            //放到集合
            dateList.add(date);
        }

        //dateList 集合存储所有显示日期
        //所有日期分页处理，每次返回每页数据
        List<Date> pageDateList = new ArrayList<>(); //每页数据list集合
        // 当前页1  每页显示 3
        int start = (page - 1) * limit;    //0
        int end = (page - 1) * limit + limit; //3
        if (end > dateList.size()) end = dateList.size();
        for (int i = start; i < end; i++) {
            pageDateList.add(dateList.get(i));
        }

        //使用IPage封装分页所有数据
        IPage<Date> iPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, limit, dateList.size());
        iPage.setRecords(pageDateList);
        return iPage;
    }

    /**
     * 将Date日期（yyyy-MM-dd HH:mm）转换为DateTime
     */
    private DateTime getDateTime(Date date, String timeString) {
        String dateTimeString = new DateTime(date).toString("yyyy-MM-dd") + " " + timeString;
        DateTime dateTime = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").parseDateTime(dateTimeString);
        return dateTime;
    }

    /**
     * 根据日期获取周几数据
     *
     * @param dateTime
     * @return
     */
    private String getDayOfWeek(DateTime dateTime) {
        String dayOfWeek = "";
        switch (dateTime.getDayOfWeek()) {
            case DateTimeConstants.SUNDAY:
                dayOfWeek = "周日";
                break;
            case DateTimeConstants.MONDAY:
                dayOfWeek = "周一";
                break;
            case DateTimeConstants.TUESDAY:
                dayOfWeek = "周二";
                break;
            case DateTimeConstants.WEDNESDAY:
                dayOfWeek = "周三";
                break;
            case DateTimeConstants.THURSDAY:
                dayOfWeek = "周四";
                break;
            case DateTimeConstants.FRIDAY:
                dayOfWeek = "周五";
                break;
            case DateTimeConstants.SATURDAY:
                dayOfWeek = "周六";
            default:
                break;
        }
        return dayOfWeek;
        
      
    }
}
