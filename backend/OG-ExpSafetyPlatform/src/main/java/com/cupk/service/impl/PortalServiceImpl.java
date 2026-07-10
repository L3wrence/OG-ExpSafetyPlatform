package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cupk.dto.RecentVisitDTO;
import com.cupk.dto.ShortcutUpdateDTO;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.PortalMapper;
import com.cupk.mapper.PortalMessageMapper;
import com.cupk.mapper.RecentVisitMapper;
import com.cupk.mapper.UserShortcutMapper;
import com.cupk.pojo.PortalMessage;
import com.cupk.pojo.RecentVisit;
import com.cupk.pojo.UserShortcut;
import com.cupk.service.PortalService;
import com.cupk.service.UserService;
import com.cupk.vo.CalendarEventVO;
import com.cupk.vo.PortalHomeVO;
import com.cupk.vo.PortalItemVO;
import com.cupk.vo.PortalMetricVO;
import com.cupk.vo.SearchResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PortalServiceImpl implements PortalService {
    private static final int DEFAULT_LIMIT = 8;

    private final PortalMapper portalMapper;
    private final PortalMessageMapper messageMapper;
    private final RecentVisitMapper recentVisitMapper;
    private final UserShortcutMapper shortcutMapper;
    private final UserService userService;

    public PortalServiceImpl(PortalMapper portalMapper,
                             PortalMessageMapper messageMapper,
                             RecentVisitMapper recentVisitMapper,
                             UserShortcutMapper shortcutMapper,
                             UserService userService) {
        this.portalMapper = portalMapper;
        this.messageMapper = messageMapper;
        this.recentVisitMapper = recentVisitMapper;
        this.shortcutMapper = shortcutMapper;
        this.userService = userService;
    }

    @Override
    public PortalHomeVO home() {
        String roleCode = primaryRole();
        PortalHomeVO home = new PortalHomeVO();
        home.setRole(roleCode.toLowerCase(Locale.ROOT));
        home.setUserInfo(userService.currentUser());

        Long userId = UserContext.userId();
        if ("ADMIN".equals(roleCode)) {
            fillAdminHome(home);
        } else {
            fillUserHome(home, userId);
            if (UserContext.isTeacher()) {
                fillTeacherHome(home, userId);
            }
        }

        home.setNotices(notices(5));
        home.setMessages(messages(5));
        home.setCalendarEvents(calendar(8));
        home.setRecentVisits(recentVisits(6));
        home.setShortcuts(shortcuts(6));
        return home;
    }

    @Override
    public List<PortalItemVO> notices(Integer limit) {
        return portalMapper.notices(primaryRole(), limit(limit)).stream().map(this::toItem).toList();
    }

    @Override
    public List<PortalItemVO> messages(Integer limit) {
        return portalMapper.messages(UserContext.userId(), limit(limit)).stream().map(this::toItem).toList();
    }

    @Override
    public Integer unreadMessages() {
        return nz(portalMapper.unreadMessages(UserContext.userId()));
    }

    @Override
    @Transactional
    public void markMessageRead(Long id) {
        PortalMessage message = messageMapper.selectById(id);
        if (message == null || Integer.valueOf(1).equals(message.getDeleted())) {
            throw new BusinessException(404, "消息不存在");
        }
        if (!UserContext.userId().equals(message.getUserId())) {
            throw new BusinessException(403, "不能操作他人的消息");
        }
        message.setReadFlag(1);
        message.setReadTime(LocalDateTime.now());
        messageMapper.updateById(message);
    }

    @Override
    public List<CalendarEventVO> calendar(Integer limit) {
        String roleCode = primaryRole();
        Long userId = UserContext.userId();
        List<CalendarEventVO> result = new ArrayList<>();
        portalMapper.studentCalendar(userId, roleCode, limit(limit)).forEach(row -> result.add(toCalendar(row)));
        portalMapper.reservationCalendar(userId, roleCode, limit(limit)).forEach(row -> result.add(toCalendar(row)));
        return result.stream().limit(limit(limit)).toList();
    }

    @Override
    public List<SearchResultVO> search(String keyword, Integer limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        String roleCode = primaryRole();
        Long teacherId = UserContext.isTeacher() ? UserContext.userId() : null;
        Long studentId = UserContext.isLearner() ? UserContext.userId() : null;
        int eachLimit = Math.max(2, limit(limit) / 4 + 1);
        List<SearchResultVO> result = new ArrayList<>();
        portalMapper.searchCourses(keyword, teacherId, studentId, roleCode, eachLimit).forEach(row -> result.add(toSearch(row)));
        portalMapper.searchExperiments(keyword, teacherId, studentId, roleCode, eachLimit).forEach(row -> result.add(toSearch(row)));
        portalMapper.searchResources(keyword, teacherId, studentId, roleCode, eachLimit).forEach(row -> result.add(toSearch(row)));
        portalMapper.searchNotices(keyword, roleCode, eachLimit).forEach(row -> result.add(toSearch(row)));
        return result.stream().limit(limit(limit)).toList();
    }

    @Override
    public List<PortalItemVO> recentVisits(Integer limit) {
        return portalMapper.recentVisits(UserContext.userId(), limit(limit)).stream().map(this::toItem).toList();
    }

    @Override
    @Transactional
    public void recordVisit(RecentVisitDTO dto) {
        Long userId = UserContext.userId();
        RecentVisit existing = recentVisitMapper.selectOne(new LambdaQueryWrapper<RecentVisit>()
                .eq(RecentVisit::getUserId, userId)
                .eq(RecentVisit::getPath, dto.getPath())
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            RecentVisit visit = new RecentVisit();
            visit.setUserId(userId);
            visit.setTitle(dto.getTitle());
            visit.setPath(dto.getPath());
            visit.setModule(dto.getModule());
            visit.setVisitCount(1);
            visit.setLastVisitTime(now);
            visit.setCreateTime(now);
            visit.setUpdateTime(now);
            recentVisitMapper.insert(visit);
        } else {
            existing.setTitle(dto.getTitle());
            existing.setModule(dto.getModule());
            existing.setVisitCount((existing.getVisitCount() == null ? 0 : existing.getVisitCount()) + 1);
            existing.setLastVisitTime(now);
            existing.setUpdateTime(now);
            recentVisitMapper.updateById(existing);
        }
    }

    @Override
    public List<PortalItemVO> shortcuts(Integer limit) {
        List<PortalItemVO> saved = portalMapper.shortcuts(UserContext.userId(), limit(limit)).stream().map(this::toItem).toList();
        return saved.isEmpty() ? defaultShortcuts(primaryRole()) : saved;
    }

    @Override
    @Transactional
    public void saveShortcuts(List<ShortcutUpdateDTO> dtos) {
        if (dtos == null || dtos.size() > 10) {
            throw new BusinessException(400, "快捷入口最多配置10个");
        }
        Long userId = UserContext.userId();
        shortcutMapper.delete(new LambdaQueryWrapper<UserShortcut>().eq(UserShortcut::getUserId, userId));
        LocalDateTime now = LocalDateTime.now();
        int index = 1;
        for (ShortcutUpdateDTO dto : dtos) {
            UserShortcut shortcut = new UserShortcut();
            shortcut.setUserId(userId);
            shortcut.setTitle(dto.getTitle());
            shortcut.setPath(dto.getPath());
            shortcut.setIcon(dto.getIcon());
            shortcut.setSort(dto.getSort() == null ? index : dto.getSort());
            shortcut.setCreateTime(now);
            shortcut.setUpdateTime(now);
            shortcutMapper.insert(shortcut);
            index++;
        }
    }

    private void fillUserHome(PortalHomeVO home, Long userId) {
        home.getMetrics().add(new PortalMetricVO("courses", "我的课堂", nz(portalMapper.countStudentCourses(userId)), "门", "/classrooms", "primary"));
        home.getMetrics().add(new PortalMetricVO("learningTasks", "待完成学习", nz(portalMapper.countStudentPendingLearningTasks(userId)), "项", "/classrooms", "warning"));
        home.getMetrics().add(new PortalMetricVO("exams", "待考试实验", nz(portalMapper.countStudentPendingExams(userId)), "项", "/classrooms", "warning"));
        home.getMetrics().add(new PortalMetricVO("reservations", "预约状态", nz(portalMapper.countStudentActiveReservations(userId)), "条", "/classrooms", "success"));
        home.getMetrics().add(new PortalMetricVO("passRate", "考试通过率", nz(portalMapper.studentExamPassRate(userId)), "%", "/classrooms", "info"));
        addRows(home.getTodos(), portalMapper.studentLearningTodos(userId, 5));
        addRows(home.getTodos(), portalMapper.studentExamTodos(userId, 4));
        addRows(home.getTodos(), portalMapper.studentReservationTodos(userId, 3));
        addRows(home.getTodos(), portalMapper.studentReportTodos(userId, 3));
        addRows(home.getTodos(), portalMapper.studentAdmissionTodos(userId, 2));
    }

    private void fillTeacherHome(PortalHomeVO home, Long teacherId) {
        int reservations = nz(portalMapper.countTeacherPendingReservations(teacherId));
        int reports = nz(portalMapper.countTeacherPendingReports(teacherId));
        home.getMetrics().add(new PortalMetricVO("pendingReservations", "待审核预约", reservations, "条", "/classrooms", "warning"));
        home.getMetrics().add(new PortalMetricVO("pendingReports", "待批改报告", reports, "份", "/classrooms", "danger"));
        home.getMetrics().add(new PortalMetricVO("lowPassWarnings", "低通过率预警", nz(portalMapper.countTeacherLowPassWarnings(teacherId)), "项", "/classrooms", "warning"));
        home.getMetrics().add(new PortalMetricVO("students", "所教学生", nz(portalMapper.countTeacherStudents(teacherId)), "人", "/classrooms", "primary"));
        addRows(home.getTodos(), portalMapper.teacherReservationTodos(teacherId, 5));
        addRows(home.getTodos(), portalMapper.teacherReportTodos(teacherId, 5));
    }

    private void fillAdminHome(PortalHomeVO home) {
        home.getMetrics().add(new PortalMetricVO("users", "用户数量", nz(portalMapper.countUsers()), "人", "/admin/users", "primary"));
        home.getMetrics().add(new PortalMetricVO("courses", "课程数量", nz(portalMapper.countCourses()), "门", "/admin/users", "success"));
        home.getMetrics().add(new PortalMetricVO("experiments", "实验项目", nz(portalMapper.countExperiments()), "项", "/admin/users", "warning"));
        home.getMetrics().add(new PortalMetricVO("logs", "今日操作日志", nz(portalMapper.countTodayLogs()), "条", "/admin/logs", "info"));
        addRows(home.getTodos(), portalMapper.recentLogs(6));
    }

    private void addRows(List<PortalItemVO> target, List<Map<String, Object>> rows) {
        rows.stream().map(this::toItem).forEach(target::add);
    }

    private PortalItemVO toItem(Map<String, Object> row) {
        PortalItemVO item = new PortalItemVO();
        item.setId(toLong(value(row, "id")));
        item.setTitle(string(value(row, "title")));
        item.setDescription(firstString(row, "description", "content"));
        item.setType(firstString(row, "type", "bizType", "biz_type"));
        item.setStatus(firstString(row, "status", "priority", "result"));
        item.setTime(string(first(row, "time", "createTime", "create_time", "publishTime", "publish_time")));
        item.setPath(firstString(row, "path"));
        item.setValue(first(row, "value", "readFlag", "read_flag", "visitCount", "visit_count"));
        return item;
    }

    private CalendarEventVO toCalendar(Map<String, Object> row) {
        CalendarEventVO event = new CalendarEventVO();
        event.setId(toLong(value(row, "id")));
        event.setTitle(string(value(row, "title")));
        event.setType(firstString(row, "type"));
        event.setStartTime(firstString(row, "startTime", "start_time"));
        event.setEndTime(firstString(row, "endTime", "end_time"));
        event.setStatus(firstString(row, "status"));
        event.setPath(firstString(row, "path"));
        return event;
    }

    private SearchResultVO toSearch(Map<String, Object> row) {
        SearchResultVO item = new SearchResultVO();
        item.setId(toLong(value(row, "id")));
        item.setTitle(string(value(row, "title")));
        item.setType(firstString(row, "type"));
        item.setDescription(firstString(row, "description"));
        item.setPath(firstString(row, "path"));
        return item;
    }

    private List<PortalItemVO> defaultShortcuts(String roleCode) {
        List<PortalItemVO> items = new ArrayList<>();
        if ("ADMIN".equals(roleCode)) {
            items.add(shortcut("用户管理", "/admin/users", "User"));
            items.add(shortcut("角色权限", "/admin/roles", "Lock"));
            items.add(shortcut("操作日志", "/admin/logs", "List"));
        } else {
            if (UserContext.isTeacher()) {
                items.add(shortcut("课堂管理", "/classrooms", "Reading"));
            }
            items.add(shortcut("我的课堂", "/classrooms", "Reading"));
            items.add(shortcut("资源学习", "/resources", "Collection"));
        }
        return items;
    }

    private PortalItemVO shortcut(String title, String path, String icon) {
        PortalItemVO item = new PortalItemVO();
        item.setTitle(title);
        item.setPath(path);
        item.setType(icon);
        return item;
    }

    private String primaryRole() {
        if (UserContext.isAdmin()) return "ADMIN";
        if (UserContext.isUser()) return "USER";
        return "USER";
    }

    private int limit(Integer limit) {
        return limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), 50);
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    private Object value(Map<String, Object> row, String key) {
        if (row.containsKey(key)) return row.get(key);
        String camel = toCamel(key);
        if (row.containsKey(camel)) return row.get(camel);
        return row.get(key.toUpperCase(Locale.ROOT));
    }

    private Object first(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object found = value(row, key);
            if (found != null) return found;
        }
        return null;
    }

    private String firstString(Map<String, Object> row, String... keys) {
        return string(first(row, keys));
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return null;
        return Long.valueOf(String.valueOf(value));
    }

    private String toCamel(String key) {
        StringBuilder builder = new StringBuilder();
        boolean upper = false;
        for (char ch : key.toCharArray()) {
            if (ch == '_') {
                upper = true;
            } else if (upper) {
                builder.append(Character.toUpperCase(ch));
                upper = false;
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}
