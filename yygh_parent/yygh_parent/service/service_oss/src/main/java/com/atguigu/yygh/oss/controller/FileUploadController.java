package com.atguigu.yygh.oss.controller;

import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.oss.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

//@Api(tags="阿里云文件管理")
@RestController
@RequestMapping("/admin/oss/file")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    //文件上传
    @PostMapping("upload")
    public R uploadFile(MultipartFile file) {
        //返回上传之后，文件在阿里云路径
        String url = fileUploadService.upload(file);
        return R.ok().data("url", url);
    }

}
