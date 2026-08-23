package com.atguigu.yygh.cmn;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.listener.ReadListener;

import java.util.Map;

public class ReadExcelListener extends AnalysisEventListener<Stu> {

    //从excel表格一行一行进行读取，把每行读取内容封装Stu对象里面
    //从第二行开始读取，认为第一行是表头
    @Override
    public void invoke(Stu stu, AnalysisContext analysisContext) {
        System.out.println(stu);
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        System.out.println("表头："+headMap);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

    }
}
