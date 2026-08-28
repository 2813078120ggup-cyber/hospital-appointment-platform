package com.atguigu.yygh.cmn.service.impl;

import com.atguigu.yygh.cmn.mapper.PlatformHelpArticleMapper;
import com.atguigu.yygh.cmn.service.PlatformHelpArticleService;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.model.cmn.PlatformHelpArticle;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlatformHelpArticleServiceImpl
        extends ServiceImpl<PlatformHelpArticleMapper, PlatformHelpArticle>
        implements PlatformHelpArticleService {

    private static final int DRAFT = 0;
    private static final int PUBLISHED = 1;
    private static final Map<String, String> CATEGORY_NAMES = new LinkedHashMap<>();

    static {
        CATEGORY_NAMES.put("registration", "预约挂号");
        CATEGORY_NAMES.put("account", "账号与实名");
        CATEGORY_NAMES.put("patient", "就诊人管理");
        CATEGORY_NAMES.put("payment", "支付与退款");
        CATEGORY_NAMES.put("cancel", "取消预约");
        CATEGORY_NAMES.put("visit", "就诊服务");
    }

    @Override
    public IPage<PlatformHelpArticle> selectAdminPage(Page<PlatformHelpArticle> pageParam,
                                                      String keyword,
                                                      String categoryCode,
                                                      Integer status) {
        LambdaQueryWrapper<PlatformHelpArticle> wrapper = buildSearchWrapper(keyword, categoryCode);
        if (status != null) {
            validateStatus(status);
            wrapper.eq(PlatformHelpArticle::getStatus, status);
        }
        wrapper.orderByDesc(PlatformHelpArticle::getStatus)
                .orderByDesc(PlatformHelpArticle::getSort)
                .orderByDesc(PlatformHelpArticle::getPublishTime)
                .orderByDesc(PlatformHelpArticle::getId);
        return page(pageParam, wrapper);
    }

    @Override
    public PlatformHelpArticle getRequired(Long id) {
        if (id == null) {
            throw new YyghException(20001, "帮助文章参数不正确");
        }
        PlatformHelpArticle article = getById(id);
        if (article == null) {
            throw new YyghException(20001, "帮助文章不存在");
        }
        return article;
    }

    @Override
    public PlatformHelpArticle saveDraft(PlatformHelpArticle input) {
        PlatformHelpArticle article = copyEditableFields(input, new PlatformHelpArticle());
        article.setStatus(DRAFT);
        article.setPublishTime(null);
        save(article);
        return article;
    }

    @Override
    public PlatformHelpArticle updateDraft(PlatformHelpArticle input) {
        if (input == null || input.getId() == null) {
            throw new YyghException(20001, "帮助文章参数不正确");
        }
        PlatformHelpArticle existing = getRequired(input.getId());
        copyEditableFields(input, existing);
        updateById(existing);
        return existing;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        PlatformHelpArticle article = getRequired(id);
        article.setStatus(status);
        if (status == PUBLISHED) {
            article.setPublishTime(new Date());
        }
        updateById(article);
    }

    @Override
    public List<PlatformHelpArticle> listPublished(String keyword, String categoryCode, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        LambdaQueryWrapper<PlatformHelpArticle> wrapper = buildSearchWrapper(keyword, categoryCode);
        wrapper.eq(PlatformHelpArticle::getStatus, PUBLISHED)
                .orderByDesc(PlatformHelpArticle::getSort)
                .orderByDesc(PlatformHelpArticle::getPublishTime)
                .orderByDesc(PlatformHelpArticle::getId)
                .last("LIMIT " + safeLimit);
        return list(wrapper);
    }

    @Override
    public List<Map<String, Object>> listPublishedCategories() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String code : CATEGORY_NAMES.keySet()) {
            counts.put(code, 0);
        }
        List<PlatformHelpArticle> articles = listPublished(null, null, 100);
        for (PlatformHelpArticle article : articles) {
            String code = article.getCategoryCode();
            if (counts.containsKey(code)) {
                counts.put(code, counts.get(code) + 1);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : CATEGORY_NAMES.entrySet()) {
            int count = counts.get(entry.getKey());
            if (count > 0) {
                Map<String, Object> category = new LinkedHashMap<>();
                category.put("code", entry.getKey());
                category.put("name", entry.getValue());
                category.put("count", count);
                result.add(category);
            }
        }
        return result;
    }

    @Override
    public PlatformHelpArticle getPublished(Long id) {
        PlatformHelpArticle article = getRequired(id);
        if (!Integer.valueOf(PUBLISHED).equals(article.getStatus())) {
            throw new YyghException(20001, "帮助文章未发布或已下线");
        }
        return article;
    }

    @Override
    public List<Map<String, String>> listCategoryOptions() {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : CATEGORY_NAMES.entrySet()) {
            Map<String, String> category = new LinkedHashMap<>();
            category.put("code", entry.getKey());
            category.put("name", entry.getValue());
            result.add(category);
        }
        return result;
    }

    private LambdaQueryWrapper<PlatformHelpArticle> buildSearchWrapper(String keyword, String categoryCode) {
        LambdaQueryWrapper<PlatformHelpArticle> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(condition -> condition.like(PlatformHelpArticle::getTitle, value)
                    .or().like(PlatformHelpArticle::getSummary, value)
                    .or().like(PlatformHelpArticle::getKeywords, value));
        }
        if (StringUtils.hasText(categoryCode)) {
            String code = categoryCode.trim();
            validateCategoryCode(code);
            wrapper.eq(PlatformHelpArticle::getCategoryCode, code);
        }
        return wrapper;
    }

    private PlatformHelpArticle copyEditableFields(PlatformHelpArticle input,
                                                    PlatformHelpArticle target) {
        if (input == null) {
            throw new YyghException(20001, "帮助文章参数不正确");
        }
        String categoryCode = requireText(input.getCategoryCode(), "请选择帮助分类");
        validateCategoryCode(categoryCode);
        String title = requireText(input.getTitle(), "请输入文章标题");
        String content = requireText(input.getContent(), "请输入文章内容");
        String summary = trimToNull(input.getSummary());
        String keywords = trimToNull(input.getKeywords());
        int sort = input.getSort() == null ? 0 : input.getSort();

        checkLength(title, 200, "文章标题不能超过 200 个字符");
        checkLength(summary, 500, "文章摘要不能超过 500 个字符");
        checkLength(content, 10000, "文章内容不能超过 10000 个字符");
        checkLength(keywords, 500, "搜索关键词不能超过 500 个字符");
        if (sort < 0 || sort > 9999) {
            throw new YyghException(20001, "展示排序必须在 0 到 9999 之间");
        }

        target.setCategoryCode(categoryCode);
        target.setCategoryName(CATEGORY_NAMES.get(categoryCode));
        target.setTitle(title);
        target.setSummary(summary);
        target.setContent(content);
        target.setKeywords(keywords);
        target.setSort(sort);
        return target;
    }

    private void validateCategoryCode(String categoryCode) {
        if (!CATEGORY_NAMES.containsKey(categoryCode)) {
            throw new YyghException(20001, "帮助分类参数不正确");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != DRAFT && status != PUBLISHED)) {
            throw new YyghException(20001, "文章状态参数不正确");
        }
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new YyghException(20001, message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void checkLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new YyghException(20001, message);
        }
    }
}
