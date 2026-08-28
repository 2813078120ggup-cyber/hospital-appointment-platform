package com.atguigu.yygh.cmn.controller;

import com.atguigu.yygh.cmn.service.DictService;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.model.cmn.Dict;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 患者门户使用的只读数据字典接口。
 */
@RestController
@RequestMapping("/api/cmn/dict")
public class ApiDictController {

    private final DictService dictService;

    public ApiDictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/findByDictCode/{dictCode}")
    public R findByDictCode(@PathVariable String dictCode) {
        List<Dict> list = dictService.getByDictCode(dictCode);
        return R.ok().data("list", list);
    }

    @GetMapping("/findChildData/{parentId}")
    public R findChildData(@PathVariable Long parentId) {
        List<Dict> list = dictService.getDataById(parentId);
        return R.ok().data("list", list);
    }
}
