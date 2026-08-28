package com.atguigu.yygh.cmn.service;

import com.atguigu.yygh.model.cmn.PlatformHelpArticle;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface PlatformHelpArticleService extends IService<PlatformHelpArticle> {

    IPage<PlatformHelpArticle> selectAdminPage(Page<PlatformHelpArticle> pageParam,
                                               String keyword,
                                               String categoryCode,
                                               Integer status);

    PlatformHelpArticle getRequired(Long id);

    PlatformHelpArticle saveDraft(PlatformHelpArticle input);

    PlatformHelpArticle updateDraft(PlatformHelpArticle input);

    void updateStatus(Long id, Integer status);

    List<PlatformHelpArticle> listPublished(String keyword, String categoryCode, int limit);

    List<Map<String, Object>> listPublishedCategories();

    PlatformHelpArticle getPublished(Long id);

    List<Map<String, String>> listCategoryOptions();
}
