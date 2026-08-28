package com.atguigu.yygh.cmn.service.impl;

import com.atguigu.yygh.cmn.mapper.PlatformNoticeMapper;
import com.atguigu.yygh.cmn.service.PlatformNoticeService;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.model.cmn.PlatformNotice;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Service
public class PlatformNoticeServiceImpl
        extends ServiceImpl<PlatformNoticeMapper, PlatformNotice>
        implements PlatformNoticeService {

    private static final int PLATFORM_NOTICE = 1;
    private static final int SUSPEND_NOTICE = 2;
    private static final int DRAFT = 0;
    private static final int PUBLISHED = 1;

    @Override
    public IPage<PlatformNotice> selectAdminPage(Page<PlatformNotice> pageParam,
                                                 String keyword,
                                                 Integer noticeType,
                                                 Integer status) {
        LambdaQueryWrapper<PlatformNotice> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(condition -> condition.like(PlatformNotice::getTitle, value)
                    .or().like(PlatformNotice::getHosname, value));
        }
        if (noticeType != null) {
            validateNoticeType(noticeType);
            wrapper.eq(PlatformNotice::getNoticeType, noticeType);
        }
        if (status != null) {
            validateStatus(status);
            wrapper.eq(PlatformNotice::getStatus, status);
        }
        wrapper.orderByDesc(PlatformNotice::getStatus)
                .orderByDesc(PlatformNotice::getSort)
                .orderByDesc(PlatformNotice::getPublishTime)
                .orderByDesc(PlatformNotice::getId);
        return page(pageParam, wrapper);
    }

    @Override
    public PlatformNotice getRequired(Long id) {
        if (id == null) {
            throw new YyghException(20001, "公告参数不正确");
        }
        PlatformNotice notice = getById(id);
        if (notice == null) {
            throw new YyghException(20001, "公告不存在");
        }
        return notice;
    }

    @Override
    public PlatformNotice saveDraft(PlatformNotice input) {
        PlatformNotice notice = copyEditableFields(input, new PlatformNotice());
        notice.setStatus(DRAFT);
        notice.setPublishTime(null);
        save(notice);
        return notice;
    }

    @Override
    public PlatformNotice updateDraft(PlatformNotice input) {
        if (input == null || input.getId() == null) {
            throw new YyghException(20001, "公告参数不正确");
        }
        PlatformNotice existing = getRequired(input.getId());
        copyEditableFields(input, existing);
        updateById(existing);
        return existing;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        validateStatus(status);
        PlatformNotice notice = getRequired(id);
        notice.setStatus(status);
        if (status == PUBLISHED) {
            notice.setPublishTime(new Date());
        }
        updateById(notice);
    }

    @Override
    public List<PlatformNotice> listPublished(Integer noticeType, int limit) {
        if (noticeType != null) {
            validateNoticeType(noticeType);
        }
        int safeLimit = Math.max(1, Math.min(limit, 20));
        LambdaQueryWrapper<PlatformNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformNotice::getStatus, PUBLISHED)
                .eq(noticeType != null, PlatformNotice::getNoticeType, noticeType)
                .orderByDesc(PlatformNotice::getSort)
                .orderByDesc(PlatformNotice::getPublishTime)
                .orderByDesc(PlatformNotice::getId)
                .last("LIMIT " + safeLimit);
        return list(wrapper);
    }

    @Override
    public PlatformNotice getPublished(Long id) {
        PlatformNotice notice = getRequired(id);
        if (!Integer.valueOf(PUBLISHED).equals(notice.getStatus())) {
            throw new YyghException(20001, "公告未发布或已下线");
        }
        return notice;
    }

    private PlatformNotice copyEditableFields(PlatformNotice input, PlatformNotice target) {
        if (input == null) {
            throw new YyghException(20001, "公告参数不正确");
        }
        validateNoticeType(input.getNoticeType());
        String title = requireText(input.getTitle(), "请输入公告标题");
        String content = requireText(input.getContent(), "请输入公告内容");
        String summary = trimToNull(input.getSummary());
        String hoscode = trimToNull(input.getHoscode());
        String hosname = trimToNull(input.getHosname());
        if (input.getNoticeType() == SUSPEND_NOTICE && !StringUtils.hasText(hosname)) {
            throw new YyghException(20001, "停诊公告必须填写医院名称");
        }
        checkLength(title, 200, "公告标题不能超过 200 个字符");
        checkLength(summary, 500, "公告摘要不能超过 500 个字符");
        checkLength(content, 5000, "公告内容不能超过 5000 个字符");
        checkLength(hoscode, 30, "医院编号不能超过 30 个字符");
        checkLength(hosname, 100, "医院名称不能超过 100 个字符");

        target.setNoticeType(input.getNoticeType());
        target.setTitle(title);
        target.setSummary(summary);
        target.setContent(content);
        target.setHoscode(input.getNoticeType() == SUSPEND_NOTICE ? hoscode : null);
        target.setHosname(input.getNoticeType() == SUSPEND_NOTICE ? hosname : null);
        target.setSort(input.getSort() == null ? 0 : input.getSort());
        return target;
    }

    private void validateNoticeType(Integer noticeType) {
        if (noticeType == null || (noticeType != PLATFORM_NOTICE && noticeType != SUSPEND_NOTICE)) {
            throw new YyghException(20001, "公告类型参数不正确");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != DRAFT && status != PUBLISHED)) {
            throw new YyghException(20001, "公告状态参数不正确");
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
