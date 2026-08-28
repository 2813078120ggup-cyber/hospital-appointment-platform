package com.atguigu.yygh.cmn.controller;

import com.atguigu.yygh.cmn.service.PlatformHelpArticleService;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.model.cmn.PlatformHelpArticle;
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
@RequestMapping("/admin/cmn/help")
public class AdminPlatformHelpArticleController {

    private final PlatformHelpArticleService platformHelpArticleService;

    public AdminPlatformHelpArticleController(PlatformHelpArticleService platformHelpArticleService) {
        this.platformHelpArticleService = platformHelpArticleService;
    }

    @GetMapping("/{page}/{limit}")
    public R list(@PathVariable Long page,
                  @PathVariable Long limit,
                  @RequestParam(required = false) String keyword,
                  @RequestParam(required = false) String categoryCode,
                  @RequestParam(required = false) Integer status) {
        long current = page == null || page < 1 ? 1 : page;
        long size = limit == null || limit < 1 ? 10 : Math.min(limit, 100);
        IPage<PlatformHelpArticle> pageModel = platformHelpArticleService.selectAdminPage(
                new Page<>(current, size), keyword, categoryCode, status);
        return R.ok().data("pageModel", pageModel);
    }

    @GetMapping("/categories")
    public R categories() {
        return R.ok().data("list", platformHelpArticleService.listCategoryOptions());
    }

    @GetMapping("/show/{id}")
    public R show(@PathVariable Long id) {
        return R.ok().data("article", platformHelpArticleService.getRequired(id));
    }

    @PostMapping
    public R save(@RequestBody PlatformHelpArticle article) {
        return R.ok().data("article", platformHelpArticleService.saveDraft(article));
    }

    @PutMapping
    public R update(@RequestBody PlatformHelpArticle article) {
        return R.ok().data("article", platformHelpArticleService.updateDraft(article));
    }

    @PutMapping("/updateStatus/{id}/{status}")
    public R updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        platformHelpArticleService.updateStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R remove(@PathVariable Long id) {
        platformHelpArticleService.getRequired(id);
        platformHelpArticleService.removeById(id);
        return R.ok();
    }
}
