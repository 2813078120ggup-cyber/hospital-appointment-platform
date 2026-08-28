package com.atguigu.yygh.cmn.controller;

import com.atguigu.yygh.cmn.service.PlatformNoticeService;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.model.cmn.PlatformNotice;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/cmn/notice")
public class AdminPlatformNoticeController {

    private final PlatformNoticeService platformNoticeService;

    public AdminPlatformNoticeController(PlatformNoticeService platformNoticeService) {
        this.platformNoticeService = platformNoticeService;
    }

    @GetMapping("/{page}/{limit}")
    public R list(@PathVariable Long page,
                  @PathVariable Long limit,
                  @RequestParam(required = false) String keyword,
                  @RequestParam(required = false) Integer noticeType,
                  @RequestParam(required = false) Integer status) {
        long current = page == null || page < 1 ? 1 : page;
        long size = limit == null || limit < 1 ? 10 : Math.min(limit, 100);
        IPage<PlatformNotice> pageModel = platformNoticeService.selectAdminPage(
                new Page<>(current, size), keyword, noticeType, status);
        return R.ok().data("pageModel", pageModel);
    }

    @GetMapping("/show/{id}")
    public R show(@PathVariable Long id) {
        return R.ok().data("notice", platformNoticeService.getRequired(id));
    }

    @PostMapping
    public R save(@RequestBody PlatformNotice notice) {
        return R.ok().data("notice", platformNoticeService.saveDraft(notice));
    }

    @PutMapping
    public R update(@RequestBody PlatformNotice notice) {
        return R.ok().data("notice", platformNoticeService.updateDraft(notice));
    }

    @PutMapping("/updateStatus/{id}/{status}")
    public R updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        platformNoticeService.updateStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R remove(@PathVariable Long id) {
        platformNoticeService.getRequired(id);
        platformNoticeService.removeById(id);
        return R.ok();
    }
}
