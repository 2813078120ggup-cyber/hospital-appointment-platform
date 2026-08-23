package com.atguigu.yygh.hosp.controller;


import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.hosp.utils.HttpRequestHelper;
import com.atguigu.yygh.model.hosp.HospitalSet;
import com.atguigu.yygh.vo.hosp.HospitalSetQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 医院设置表 前端控制器
 */
//@Api(tags = "医院设置管理接口")
@RestController
@RequestMapping("/admin/hosp/hospitalSet")
//@CrossOrigin
public class HospitalSetController {

    //注入service
    @Autowired
    private HospitalSetService hospitalSetService;

    //9 医院设置锁定和解锁
    @PutMapping("lockHospitalSet/{id}/{status}")
    public R lockHospitalSet(@PathVariable Long id,
                             @PathVariable Integer status) {
        //根据id查询医院设置信息
        HospitalSet hospitalSet = hospitalSetService.getById(id);
        //设置状态
        hospitalSet.setStatus(status);
        //调用方法
        hospitalSetService.updateById(hospitalSet);
        return R.ok();
    }

    //8 批量删除
    /**
     * json对象格式：{...}  -- java对象
     * <p>
     * json数组格式 [1,2,3] -- java的list集合
     */
    //@ApiOperation("批量删除")
    @DeleteMapping("batchRemove")
    public R deleteBatch(@RequestBody List<Long> idList) {
        // 使用@RequestBody 获取json数组格式，数组有多个id值 [1,2,3]
        hospitalSetService.removeByIds(idList);
        return R.ok();
    }

    //7 修改接口- 最终实现
    //@ApiOperation("修改")
    @PutMapping("updateHospSet")
    public R updateHospset(@RequestBody HospitalSet hospitalSet) {
        boolean is_success = hospitalSetService.updateById(hospitalSet);
        if (is_success) {
            return R.ok();
        } else {
            return R.error();
        }
    }

    @PostMapping("addHospSet")
    public R addHospSet(@RequestBody HospitalSet hospitalSet) {
        hospitalSet.setSignKey("1");
        boolean is_success = hospitalSetService.save(hospitalSet);
        if (is_success) {
            return R.ok();
        } else {
            return R.error();
        }
    }

    //6 修改接口- 根据id查询  getHospSetById/1
    //@ApiOperation("根据id查询")
    @GetMapping("getHospSetById/{id}")
    public R getHospSetById(@PathVariable Long id) {
        HospitalSet hospitalSet = hospitalSetService.getById(id);
        return R.ok().data("hospitalSet", hospitalSet);
    }

    //5 添加接口 restful
    //@ApiOperation("添加")
    @PostMapping("saveHospSet")
    public R saveHospset(@RequestBody HospitalSet hospitalSet) {
        //为每个医院生成唯一字符串
        String signKey = System.currentTimeMillis() + "" + new Random().nextInt(1000);
        //设置到hospitalSet
        hospitalSet.setSignKey(signKey);
        //添加方法调用
        boolean is_success = hospitalSetService.save(hospitalSet);
        if (is_success) { //成功
            //把为这个医院生成唯一字符串保存到医院系统中
            //平台调用医院模拟系统同步接口实现
            Map<String, Object> map = new HashMap<>();
            map.put("sign", signKey); //签名秘钥
            map.put("hoscode", hospitalSet.getHoscode()); //医院编号
            //使用httpclient调用，封装了工具类
            HttpRequestHelper.sendRequest(map, "http://localhost:9998/hospSet/updateSignKey");
            return R.ok();
        } else {
            return R.error();
        }
    }

    //4 条件分页查询

    /**
     * RequestBody(required = false)
     * <p>
     * 这个注解作用：
     * 传递数据时候以json格式
     * 使用这个注解获取json格式数据，把json个数据封装到对象里面
     * <p>
     * 通俗：接受json格式数据进行封装
     * <p>
     * 这个注解使用时候，请求方式不能get
     */
    //@ApiOperation("条件分页查询requestbody")
    @PostMapping("findPageQueryHospSet/{current}/{limit}")
    public R findPageQueryHospSet(@PathVariable long current,
                                  @PathVariable long limit,
                                  @RequestBody(required = false) HospitalSetQueryVo hospitalSetQueryVo) {
        //创建page对象，传递current当前页   limit每页显示记录数
        Page<HospitalSet> pageParam = new Page<>(current, limit);
        //封装条件
        if (hospitalSetQueryVo == null) { //如果条件对象为空，查询全部
            //调用方法实现分页查询
            hospitalSetService.page(pageParam);
        } else {  //如果条件对象不为空，进行条件查询
            QueryWrapper<HospitalSet> wrapper = new QueryWrapper<>();
            //判断条件值是否为空，不为空封装
            String hoscode = hospitalSetQueryVo.getHoscode();
            String hosname = hospitalSetQueryVo.getHosname();
            if (!StringUtils.isEmpty(hoscode)) {
                wrapper.eq("hoscode", hoscode);
            }
            if (!StringUtils.isEmpty(hosname)) {
                wrapper.like("hosname", hosname);
            }
            //调用方法
            hospitalSetService.page(pageParam, wrapper);
        }
        //获取数据
        List<HospitalSet> list = pageParam.getRecords();
        long total = pageParam.getTotal();
        //返回数据
        return R.ok().data("total", total).data("list", list);
    }

    //4 条件分页查询
    //@ApiOperation("条件分页查询")
    @GetMapping("findPageQuery/{current}/{limit}")
    public R findPageQuery(@PathVariable long current,
                           @PathVariable long limit,
                           HospitalSetQueryVo hospitalSetQueryVo) {
        //创建page对象，传递current当前页   limit每页显示记录数
        Page<HospitalSet> pageParam = new Page<>(current, limit);
        //封装条件
        if (hospitalSetQueryVo == null) { //如果条件对象为空，查询全部
            //调用方法实现分页查询
            hospitalSetService.page(pageParam);
        } else {  //如果条件对象不为空，进行条件查询
            QueryWrapper<HospitalSet> wrapper = new QueryWrapper<>();
            //判断条件值是否为空，不为空封装
            String hoscode = hospitalSetQueryVo.getHoscode();
            String hosname = hospitalSetQueryVo.getHosname();
            if (!StringUtils.isEmpty(hoscode)) {
                wrapper.eq("hoscode", hoscode);
            }
            if (!StringUtils.isEmpty(hosname)) {
                wrapper.like("hosname", hosname);
            }
            //调用方法
            hospitalSetService.page(pageParam, wrapper);
        }
        //获取数据
        List<HospitalSet> list = pageParam.getRecords();
        long total = pageParam.getTotal();
        //返回数据
        return R.ok().data("total", total).data("list", list);
    }

    //3 分页查询（不带条件）
    // current当前页   limit每页显示记录数
    //@ApiOperation("分页查询")
    @GetMapping("findPage/{current}/{limit}")
    public R findPage(@PathVariable long current,
                      @PathVariable long limit) {
        //创建page对象，传递current当前页   limit每页显示记录数
        Page<HospitalSet> pageParam = new Page<>(current, limit);
        //调用方法实现分页查询
        hospitalSetService.page(pageParam);
        //pageParam封装分页所有数据
        List<HospitalSet> list = pageParam.getRecords();
        long total = pageParam.getTotal();
        //返回数据
        return R.ok().data("total", total).data("list", list);
//        Map<String,Object> map = new HashMap<>();
//        map.put("total",total);
//        map.put("list",list);
//        return R.ok().data(map);
    }

    // http://localhost:8201/admin/hosp/hospitalSet/findAll
    //1 查询医院设置表所有数据
    // restful
    //@ApiOperation("查询所有数据")
    @GetMapping("findAll")
    public R findAllHospSet() {
        //模拟异常
        try {
            int i = 9 / 0;
        } catch (Exception e) {
            //手动抛出异常
            //  throw  throws
            throw new YyghException(20001, "执行自定义异常处理!!");
        }

        //调用service方法
        List<HospitalSet> list = hospitalSetService.list();
        return R.ok().data("list", list);
    }

    //2 逻辑删除医院设置
    //@ApiOperation("逻辑删除")
    @DeleteMapping("remove/{id}")
    public R removeHospSet(@PathVariable Long id) {
        boolean is_success = hospitalSetService.removeById(id);
        if (is_success) {
            return R.ok();
        } else {
            return R.error();
        }
    }
}

