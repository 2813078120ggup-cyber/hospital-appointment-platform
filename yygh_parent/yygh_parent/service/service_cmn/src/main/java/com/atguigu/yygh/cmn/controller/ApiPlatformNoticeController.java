package com.atguigu.yygh.cmn.controller;

import com.atguigu.yygh.cmn.service.PlatformNoticeService;
import com.atguigu.yygh.common.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cmn/notice")
public class ApiPlatformNoticeController {

    private final PlatformNoticeService platformNoticeService;

    public ApiPlatformNoticeController(PlatformNoticeService platformNoticeService) {
        this.platformNoticeService = platformNoticeService;
    }

    @GetMapping("/list")
    public R list(@RequestParam(required = false) Integer noticeType,
                  @RequestParam(defaultValue = "10") Integer limit) {
        int safeLimit = limit == null ? 10 : limit;
        return R.ok().data("list", platformNoticeService.listPublished(noticeType, safeLimit));
    }

    @GetMapping("/{id}")
    public R show(@PathVariable Long id) {
        return R.ok().data("notice", platformNoticeService.getPublished(id));
    }
}
