package com.atguigu.yygh.hosp.repository;

import com.atguigu.yygh.model.hosp.Department;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends MongoRepository<Department, String> {

    // 根据hoscode + depcode查询
    Department getDeptByHoscodeAndDepcode(String hoscode, String depcode);

    // 根据科室名称查询（不同医院可能存在同名科室）
    List<Department> findAllByDepname(String depname);
}
