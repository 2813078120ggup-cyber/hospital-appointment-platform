package com.atguigu.yygh.hosp.controller;

import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.vo.hosp.HospitalQueryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

//@Api(tags = "医院接口")
@RestController
@RequestMapping("/admin/hosp/hospital")
//@CrossOrigin
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    //医院条件分页查询
    //@ApiOperation(value = "医院获取分页列表")
    @GetMapping("{page}/{limit}")
    public R index(@PathVariable Integer page,
                   @PathVariable Integer limit,
                   HospitalQueryVo hospitalQueryVo) {
        //调用service方法
        Page<Hospital> pageModel =
                hospitalService.selectPageHosp(page,limit,hospitalQueryVo);
        return R.ok().data("pages",pageModel);
    }

    //@ApiOperation(value = "更新上线状态")
    @GetMapping("updateStatus/{id}/{status}")
    public R lock(@PathVariable("id") String id,
            @PathVariable("status") Integer status){
        hospitalService.updateStatus(id, status);
        return R.ok();
    }

    //@ApiOperation(value = "获取医院详情")
    @GetMapping("show/{id}")
    public R show(@PathVariable String id) {
        Map<String, Object> map = hospitalService.showHosp(id);
        return R.ok().data(map);
    }
}
