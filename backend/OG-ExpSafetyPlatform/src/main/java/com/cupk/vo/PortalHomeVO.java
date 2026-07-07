package com.cupk.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PortalHomeVO {
    private String role;
    private UserInfoVO userInfo;
    private List<PortalMetricVO> metrics = new ArrayList<>();
    private List<PortalItemVO> todos = new ArrayList<>();
    private List<PortalItemVO> notices = new ArrayList<>();
    private List<PortalItemVO> messages = new ArrayList<>();
    private List<CalendarEventVO> calendarEvents = new ArrayList<>();
    private List<PortalItemVO> recentVisits = new ArrayList<>();
    private List<PortalItemVO> shortcuts = new ArrayList<>();
}
