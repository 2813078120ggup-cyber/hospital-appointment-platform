package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.cmn.client.DictFeignClient;
import com.atguigu.yygh.enums.DictEnum;
import com.atguigu.yygh.hosp.repository.HospitalRepository;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.vo.hosp.HospitalQueryVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HospitalServiceImpl implements HospitalService {

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private DictFeignClient dictFeignClient;

    //添加医院数据
    @Override
    public void saveHosp(Map<String, Object> newObjectMap) {
        // newObjectMap -- Hospital
        //json工具实现
        //1 newObjectMap转换json字符串
        String jsonString = JSONObject.toJSONString(newObjectMap);
        //json字符串转换Hospital对象
        Hospital hospital = JSONObject.parseObject(jsonString, Hospital.class);

        //2 判断当前医院数据是否已经添加，如果添加，进行修改，没有添加直接添加
        //根据医院编号查询
        Hospital existHospital = hospitalRepository.findByHoscode(hospital.getHoscode());
        if (existHospital != null) {  //已经添加过了，修改
            //设置id值
            hospital.setId(existHospital.getId());
            hospital.setCreateTime(existHospital.getCreateTime());
            hospital.setUpdateTime(new Date());
            //调用方法添加
            hospitalRepository.save(hospital);
        } else {  //没有添加直接添加
            //调用方法添加
            hospital.setCreateTime(new Date());
            hospital.setUpdateTime(new Date());
            hospitalRepository.save(hospital);
        }
    }

    //医院编号查询
    @Override
    public Hospital getHosp(String hoscode) {
        Hospital hospital = hospitalRepository.findByHoscode(hoscode);
        return hospital;
    }

    //医院条件分页查询
    @Override
    public Page<Hospital> selectPageHosp(Integer page, Integer limit,
                                         HospitalQueryVo hospitalQueryVo) {
        //设置排序
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");

        //设置分页
        Pageable pageable = PageRequest.of(page - 1, limit, sort);

        //封装条件
        ExampleMatcher matcher = ExampleMatcher.matching() //构建对象
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING) //改变默认字符串匹配方式：模糊查询
                .withIgnoreCase(true); //改变默认大小写忽略方式：忽略大小写

        // hospitalQueryVo - hospital
        Hospital hospital = new Hospital();
        BeanUtils.copyProperties(hospitalQueryVo, hospital);
        Example<Hospital> example = Example.of(hospital, matcher);

        //调用方法得到
        Page<Hospital> pageModel = hospitalRepository.findAll(example, pageable);

        //获取查询list集合
        pageModel.getContent().stream().forEach(item -> {
            //遍历list集合，得到每个Hospital对象
            this.packHospital(item);
        });
        return pageModel;
    }

    //更新上线状态
    @Override
    public void updateStatus(String id, Integer status) {
        if (status.intValue() == 0 || status.intValue() == 1) {
            Hospital hospital = hospitalRepository.findById(id).get();
            hospital.setStatus(status);
            hospital.setUpdateTime(new Date());
            hospitalRepository.save(hospital);
        }
    }

    //获取医院详情
    @Override
    public Map<String, Object> showHosp(String id) {
        //根据id查询
        Hospital hospital = this.packHospital(hospitalRepository.findById(id).get());

        Map<String, Object> result = new HashMap<>();
        //医院基本信息（包含医院等级）
        result.put("hospital", hospital);
        //单独处理更直观
        result.put("bookingRule", hospital.getBookingRule());
        return result;
    }

    //医院名称模糊查询
    @Override
    public List<Hospital> getHospLike(String hosname) {
        List<Hospital> list = hospitalRepository.findHospitalByHosnameLike(hosname);
        return list;
    }

    //根据医院编号获取医院详情
    @Override
    public Map<String, Object> selectHospByHoscode(String hoscode) {
        Map<String, Object> result = new HashMap<>();
        Hospital hosp = this.getHosp(hoscode);
        //医院详情
        Hospital hospital = this.packHospital(hosp);
        result.put("hospital", hospital);
        //预约规则
        result.put("bookingRule", hospital.getBookingRule());
        return result;
    }

    //获取每个对象编号，远程调用根据编号获取名称，把获取名称封装Hospital对象的map里面
    private Hospital packHospital(Hospital hospital) {
        //获取每个对象编号
        String hostype = hospital.getHostype();//医院等级
        //省 市  区
        String provinceCode = hospital.getProvinceCode();
        String cityCode = hospital.getCityCode();
        String districtCode = hospital.getDistrictCode();
        //远程调用根据编号获取对应名称
        String provinceString = dictFeignClient.getName(provinceCode);
        String cityString = dictFeignClient.getName(cityCode);
        String districtString = dictFeignClient.getName(districtCode);
        //医院等级名称
        String hostypeString = dictFeignClient.getName(DictEnum.HOSTYPE.getDictCode(), hostype);

        //数据封装map
        hospital.getParam().put("hostypeString", hostypeString);
        hospital.getParam().put("fullAddress", provinceString + cityString + districtString + hospital.getAddress());
        return hospital;
    }
}
