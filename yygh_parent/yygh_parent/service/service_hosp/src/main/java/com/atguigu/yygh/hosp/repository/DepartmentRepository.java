package com.atguigu.yygh.hosp.repository;

import com.atguigu.yygh.model.hosp.Department;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends MongoRepository<Department, String> {

    // 根据hoscode + depcode查询
    Department getDeptByHoscodeAndDepcode(String hoscode, String depcode);

    // ---- depname（小科室名）查询 ----
    // 精确匹配
    List<Department> findAllByDepname(String depname);
    // 模糊匹配（包含即可）
    List<Department> findAllByDepnameContaining(String keyword);

    // ---- bigname（大科室名）查询 ----
    // 精确匹配
    List<Department> findAllByBigname(String bigname);
    // 模糊匹配（包含即可）
    List<Department> findAllByBignameContaining(String keyword);
}
