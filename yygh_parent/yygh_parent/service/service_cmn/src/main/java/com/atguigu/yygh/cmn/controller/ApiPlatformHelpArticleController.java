package com.atguigu.yygh.cmn.controller;

import com.atguigu.yygh.cmn.service.PlatformHelpArticleService;
import com.atguigu.yygh.common.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cmn/help")
public class ApiPlatformHelpArticleController {

    private final PlatformHelpArticleService platformHelpArticleService;

    public ApiPlatformHelpArticleController(PlatformHelpArticleService platformHelpArticleService) {
        this.platformHelpArticleService = platformHelpArticleService;
    }

    @GetMapping("/list")
    public R list(@RequestParam(required = false) String keyword,
                  @RequestParam(required = false) String categoryCode,
                  @RequestParam(defaultValue = "100") Integer limit) {
        int safeLimit = limit == null ? 100 : limit;
        return R.ok().data("list",
                platformHelpArticleService.listPublished(keyword, categoryCode, safeLimit));
    }

    @GetMapping("/categories")
    public R categories() {
        return R.ok().data("list", platformHelpArticleService.listPublishedCategories());
    }

    @GetMapping("/{id}")
    public R show(@PathVariable Long id) {
        return R.ok().data("article", platformHelpArticleService.getPublished(id));
    }
}
