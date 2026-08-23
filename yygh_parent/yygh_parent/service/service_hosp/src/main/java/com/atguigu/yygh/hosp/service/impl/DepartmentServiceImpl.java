package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.hosp.repository.DepartmentRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.vo.hosp.DepartmentVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    //上传科室
    @Override
    public void saveDept(Map<String, Object> newObjectMap) {
        //newObjectMap --  Department
        String jsonString = JSONObject.toJSONString(newObjectMap);
        Department department = JSONObject.parseObject(jsonString, Department.class);

        //是否存在科室，存在更新，没有存在添加
        // 根据hoscode + depcode查询
        Department existDepartment = departmentRepository.getDeptByHoscodeAndDepcode(department.getHoscode(),
                department.getDepcode());
        if (existDepartment != null) { //更新
            //设置id
            department.setId(existDepartment.getId());
            department.setCreateTime(existDepartment.getCreateTime());
            department.setUpdateTime(new Date());
            departmentRepository.save(department);
        } else {
            department.setCreateTime(new Date());
            department.setUpdateTime(new Date());
            departmentRepository.save(department);
        }

    }

    //科室获取分页列表
    @Override
    public Page<Department> selectPageDept(int page, int limit, String hoscode, String depcode) {
        //设置排序规则
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");

        //设置分页参数
        //第一页 0
        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        //设置条件
        Department department = new Department();
        department.setHoscode(hoscode);
        department.setDepcode(depcode);
        Example<Department> example = Example.of(department);

        //调用方法分页查询
        Page<Department> pageModel = departmentRepository.findAll(pageable);

        //返回
        return pageModel;
    }

    //删除科室
    @Override
    public void remove(String hoscode, String depcode) {
        //hoscode+depcode查询id
        Department dept =
                departmentRepository.getDeptByHoscodeAndDepcode(hoscode, depcode);
        if (dept != null) {
            departmentRepository.deleteById(dept.getId());
        }
    }

    //根据医院编号查询医院所有科室，按照树形显示
    @Override
    public List<DepartmentVo> getDeptTree(String hoscode) {
        //1 根据医院编号查询医院所有科室
        Department departmentQuery = new Department();
        departmentQuery.setHoscode(hoscode);
        Example<Department> example = Example.of(departmentQuery);
        // findDeptByHoscode(hoscode)
        List<Department> departmentList = departmentRepository.findAll(example);

        //2 按照大科室（编号）进行分组处理，使用stream实现，转换map集合
        //map集合 key：分组大科室编号   value：每组数据list集合
        Map<String, List<Department>> deparmentMap =
                departmentList.stream().collect(Collectors.groupingBy(Department::getBigcode));

        //放最终封装数据
        List<DepartmentVo> finalDeptList = new ArrayList<>();

        //3 遍历map集合，封装大科室，封装小科室
        for (Map.Entry<String, List<Department>> entry : deparmentMap.entrySet()) {
            //获取map里面每个key和value
            String bigCode = entry.getKey();
            List<Department> deptList = entry.getValue();

            //封装大科室
            DepartmentVo departmentVoBig = new DepartmentVo();
            //大科室编号
            departmentVoBig.setDepcode(bigCode);
            //大科室名称
            String bigname = deptList.get(0).getBigname();
            departmentVoBig.setDepname(bigname);

            //向每个大科室里面封装小科室
            // 类型转换 List<Department> -- List<DepartmentVo>
            //创建list集合封装小科室
            List<DepartmentVo> deptListSmall = new ArrayList<>();
            for (Department department : deptList) {
                // DepartmentVo
                DepartmentVo departmentVo = new DepartmentVo();
                //BeanUtils.copyProperties(department,departmentVo);
                departmentVo.setDepcode(department.getDepcode());
                departmentVo.setDepname(department.getDepname());
                deptListSmall.add(departmentVo);
            }

            //把小科室放到大科室里面
            departmentVoBig.setChildren(deptListSmall);

            //多个大科室放到最终list集合
            finalDeptList.add(departmentVoBig);
        }
        return finalDeptList;
    }

    @Override
    public Department getDepartment(String hoscode, String depcode) {
        Department dept = departmentRepository.getDeptByHoscodeAndDepcode(hoscode, depcode);
        return dept;
    }

}
