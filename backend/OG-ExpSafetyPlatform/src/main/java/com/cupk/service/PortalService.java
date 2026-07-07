package com.cupk.service;

import com.cupk.dto.RecentVisitDTO;
import com.cupk.dto.ShortcutUpdateDTO;
import com.cupk.vo.CalendarEventVO;
import com.cupk.vo.PortalHomeVO;
import com.cupk.vo.PortalItemVO;
import com.cupk.vo.SearchResultVO;

import java.util.List;

public interface PortalService {
    PortalHomeVO home();
    List<PortalItemVO> notices(Integer limit);
    List<PortalItemVO> messages(Integer limit);
    Integer unreadMessages();
    void markMessageRead(Long id);
    List<CalendarEventVO> calendar(Integer limit);
    List<SearchResultVO> search(String keyword, Integer limit);
    List<PortalItemVO> recentVisits(Integer limit);
    void recordVisit(RecentVisitDTO dto);
    List<PortalItemVO> shortcuts(Integer limit);
    void saveShortcuts(List<ShortcutUpdateDTO> dtos);
}
