package com.atguigu.yygh.cmn.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.listener.ReadListener;
import com.atguigu.yygh.cmn.mapper.DictMapper;
import com.atguigu.yygh.model.cmn.Dict;
import com.atguigu.yygh.vo.cmn.DictEeVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 通过spring管理
 */
@Component
public class DictListener extends AnalysisEventListener<DictEeVo> {

    //注入mapper
    @Autowired
    private DictMapper dictMapper;

    //从excel第二行开始读取，一行一行读取，把每行内容封装到对象里面
    @Override
    public void invoke(DictEeVo dictEeVo, AnalysisContext analysisContext) {
        // dictEeVo对象 每行封装数据对象
        //把每行数据添加数据库里面
        // DictEeVo  ---   Dict
        Dict dict = new Dict();
        BeanUtils.copyProperties(dictEeVo,dict);
        dictMapper.insert(dict);
    }

    //在操作完成之后执行
    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

    }
}
