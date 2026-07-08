package com.cupk.service;

import com.cupk.dto.ResourceTimelineNoteDTO;
import com.cupk.vo.ResourceTimelineNoteVO;
import com.cupk.vo.ResourceTimelineStatsVO;

import java.util.List;

public interface ResourceTimelineNoteService {
    List<ResourceTimelineNoteVO> listByResource(Long resourceId, Boolean mineOnly);
    Long create(ResourceTimelineNoteDTO dto);
    void delete(Long id);
    List<ResourceTimelineStatsVO> hotspots(Long experimentId);
}
