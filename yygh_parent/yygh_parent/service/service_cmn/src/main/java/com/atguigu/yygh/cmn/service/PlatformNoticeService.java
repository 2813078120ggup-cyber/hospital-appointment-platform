package com.atguigu.yygh.cmn.service;

import com.atguigu.yygh.model.cmn.PlatformNotice;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PlatformNoticeService extends IService<PlatformNotice> {

    IPage<PlatformNotice> selectAdminPage(Page<PlatformNotice> pageParam,
                                          String keyword,
                                          Integer noticeType,
                                          Integer status);

    PlatformNotice getRequired(Long id);

    PlatformNotice saveDraft(PlatformNotice input);

    PlatformNotice updateDraft(PlatformNotice input);

    void updateStatus(Long id, Integer status);

    List<PlatformNotice> listPublished(Integer noticeType, int limit);

    PlatformNotice getPublished(Long id);
}
