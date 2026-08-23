package com.atguigu.yygh.cmn.controller;


import com.atguigu.yygh.cmn.service.DictService;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.model.cmn.Dict;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


/**
 * 组织架构表 前端控制器
 */
//@Api(tags = "数据字典接口")
@RestController
@RequestMapping("/admin/cmn/dict")
//@CrossOrigin //跨域
public class DictController {

    @Autowired
    private DictService dictService;

    //查询所有省（学历、医院等级等）
    //@ApiOperation(value = "根据dictCode获取下级节点")
    @GetMapping(value = "/findByDictCode/{dictCode}")
    public R findByDictCode(@PathVariable String dictCode) {
        List<Dict> list = dictService.getByDictCode(dictCode);
        return R.ok().data("list", list);
    }

    //@ApiOperation(value = "获取数据字典名称")
    @GetMapping(value = "/getName/{parentDictCode}/{value}")
    public String getName(@PathVariable("parentDictCode") String parentDictCode,
                          @PathVariable("value") String value) {
        return dictService.getNameByValue(parentDictCode, value);
    }

    //@ApiOperation(value = "获取数据字典名称")
    @GetMapping(value = "/getName/{value}")
    public String getName(@PathVariable("value") String value) {
        return dictService.getNameByValue("", value);
    }

//    @ApiOperation("导入")
//    @PostMapping("importDataNew")
//    public R importDictDataNew(MultipartFile file) {
//        try {
//            //new DictListenerNew(dictService)
//            EasyExcel.read(file.getInputStream(),
//                           DictEeVo.class,
//                            new DictListenerNew(dictService))
//                     .sheet()
//                     .doRead();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return R.ok();
//    }

    //导入   上传
    // <input type="file" name="file"/>
    //@ApiOperation("导入")
    @PostMapping("importData")
    public R importDictData(MultipartFile file) {
        //获取上传文件   MultipartFile
        dictService.importDataDict(file);
        return R.ok();
    }

    //导出
    //@ApiOperation(value="导出")
    @GetMapping(value = "/exportData")
    public void exportData(HttpServletResponse response) {
        dictService.exportDictData(response);
    }

    //列表接口
    // 懒加载效果，每次显示一层数据，根据id查询一层数据
    // 语句 SELECT * FROM dict WHERE parent_id=?
    //@ApiOperation("数据字典列表")
    @GetMapping("findDataById/{id}")
    public R findDataById(@PathVariable Long id) {
        //调用service的方法
        List<Dict> list = dictService.getDataById(id);
        return R.ok().data("list", list);
    }
}

