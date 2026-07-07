package com.cupk.service;

import com.cupk.common.PageResult;
import com.cupk.dto.DiscussionReplyCreateDTO;
import com.cupk.dto.DiscussionTopicCreateDTO;
import com.cupk.vo.DiscussionTopicVO;

public interface DiscussionService {
    PageResult<DiscussionTopicVO> page(Long courseId, Long experimentId, String status, int pageNum, int pageSize);
    DiscussionTopicVO detail(Long id);
    Long create(DiscussionTopicCreateDTO dto);
    Long reply(Long topicId, DiscussionReplyCreateDTO dto);
    void updateStatus(Long topicId, String status);
    void updateFeatured(Long topicId, Integer featured);
}
