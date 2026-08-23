package com.atguigu.yygh.hosp.repository;

import com.atguigu.yygh.model.hosp.Hospital;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalRepository extends MongoRepository<Hospital, String> {

    //根据医院编号查询
    Hospital findByHoscode(String hoscode);

    //医院名称模糊查询
    List<Hospital> findHospitalByHosnameLike(String hosname);
}
