package com.atguigu.hospital.service;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.Map;

public interface ApiService {

    String getHoscode();

    String getSignKey();

    JSONObject getHospital();

    boolean saveHospital(String data);

    Map<String, Object> findDepartment(int pageNum, int pageSize);

    Map<String, Object> findDepartment(int pageNum, int pageSize, String depcode);

    boolean saveDepartment(String data);

    boolean removeDepartment(String depcode);

    Map<String, Object> findSchedule(int pageNum, int pageSize);

    Map<String, Object> findSchedule(int pageNum, int pageSize, String depcode,
                                     String doctorName, String workDate,
                                     Integer status, String hosScheduleId);

    boolean saveSchedule(String data);

    boolean removeSchedule(String hosScheduleId);

    boolean suspendSchedule(String hosScheduleId, Integer status);

    Map<String, Object> findFeedback(int pageNum, int pageSize);

    boolean handleFeedback(Long id, Integer status, String reply);

    void saveBatchHospital() throws IOException;
}
