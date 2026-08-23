package com.atguigu.yygh.cmn;

import com.alibaba.excel.EasyExcel;

public class TestRead {

    public static void main(String[] args) {
        //1 指定文件位置和名称
        String fileName = "D:\\0620.xlsx";
        //2 调用方法
        EasyExcel.read(fileName,Stu.class,new ReadExcelListener()).sheet().doRead();
    }
}
