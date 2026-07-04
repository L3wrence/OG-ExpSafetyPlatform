package com.cupk.service.impl;

import com.cupk.dto.DashboardQueryDTO;
import com.cupk.exception.BusinessException;
import com.cupk.common.UserContext;
import com.cupk.mapper.DashboardMapper;
import com.cupk.service.DashboardService;
import com.cupk.vo.CapacityUsageVO;
import com.cupk.vo.CourseCompletionVO;
import com.cupk.vo.DashboardOverviewVO;
import com.cupk.vo.ExamPassRateVO;
import com.cupk.vo.PieItemVO;
import com.cupk.vo.ResourceCompletionVO;
import com.cupk.vo.ResourceRankingVO;
import com.cupk.vo.TrendItemVO;
import com.cupk.vo.WrongKnowledgeRankingVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {
    private final DashboardMapper dashboardMapper;

    public DashboardServiceImpl(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    @Override
    public DashboardOverviewVO overview(DashboardQueryDTO dto) {
        Scope scope = scope(dto);
        return dashboardMapper.selectOverview(scope.teacherId(), scope.studentId(), dto.getCourseId(),
                dto.getExperimentId(), LocalDate.now().withDayOfMonth(1).atStartOfDay());
    }

    @Override
    public List<CourseCompletionVO> courseCompletion(DashboardQueryDTO dto) {
        Scope scope = scope(dto);
        return dashboardMapper.selectCourseCompletion(scope.teacherId(), scope.studentId(), dto.getCourseId(),
                dto.getExperimentId());
    }

    @Override
    public List<PieItemVO> resourceTypeDistribution(DashboardQueryDTO dto) {
        Scope scope = scope(dto);
        return dashboardMapper.selectResourceTypeDistribution(scope.teacherId(), dto.getCourseId(), dto.getExperimentId());
    }

    @Override
    public List<ResourceRankingVO> hotResources(DashboardQueryDTO dto) {
        Scope scope = scope(dto);
        return dashboardMapper.selectHotResources(scope.teacherId(), dto.getCourseId(), dto.getExperimentId(), limit(dto));
    }

    @Override
    public List<ResourceCompletionVO> resourceCompletion(DashboardQueryDTO dto) {
        Scope scope = scope(dto);
        return dashboardMapper.selectResourceCompletion(scope.teacherId(), scope.studentId(), dto.getCourseId(),
                dto.getExperimentId());
    }

    @Override
    public List<ExamPassRateVO> examPassRate(DashboardQueryDTO dto) {
        Scope scope = scope(dto);
        Period period = period(dto);
        return dashboardMapper.selectExamPassRate(scope.teacherId(), scope.studentId(), dto.getCourseId(),
                dto.getExperimentId(), period.startTime(), period.endTime());
    }

    @Override
    public List<WrongKnowledgeRankingVO> wrongKnowledgeRanking(DashboardQueryDTO dto) {
        Scope scope = scope(dto);
        Period period = period(dto);
        return dashboardMapper.selectWrongKnowledgeRanking(scope.teacherId(), scope.studentId(), dto.getCourseId(),
                dto.getExperimentId(), period.startTime(), period.endTime(), limit(dto));
    }

    @Override
    public List<TrendItemVO> reservationTrend(DashboardQueryDTO dto) {
        Scope scope = scope(dto);
        Period period = period(dto);
        return dashboardMapper.selectReservationTrend(scope.teacherId(), scope.studentId(), dto.getCourseId(),
                dto.getExperimentId(), period.startTime(), period.endTime());
    }

    @Override
    public List<PieItemVO> reservationStatusDistribution(DashboardQueryDTO dto) {
        Scope scope = scope(dto);
        Period period = period(dto);
        return dashboardMapper.selectReservationStatusDistribution(scope.teacherId(), scope.studentId(), dto.getCourseId(),
                dto.getExperimentId(), period.startTime(), period.endTime());
    }

    @Override
    public List<CapacityUsageVO> capacityUsage(DashboardQueryDTO dto) {
        Scope scope = scope(dto);
        Period period = period(dto);
        return dashboardMapper.selectCapacityUsage(scope.teacherId(), dto.getCourseId(), dto.getExperimentId(),
                period.startTime(), period.endTime(), limit(dto));
    }

    @Override
    public List<PieItemVO> reportScoreDistribution(DashboardQueryDTO dto) {
        Scope scope = scope(dto);
        Period period = period(dto);
        return dashboardMapper.selectReportScoreDistribution(scope.teacherId(), scope.studentId(), dto.getCourseId(),
                dto.getExperimentId(), period.startTime(), period.endTime());
    }

    @Override
    public List<TrendItemVO> learningActivityTrend(DashboardQueryDTO dto) {
        Scope scope = scope(dto);
        Period period = period(dto);
        return dashboardMapper.selectLearningActivityTrend(scope.teacherId(), scope.studentId(), dto.getCourseId(),
                dto.getExperimentId(), period.startTime(), period.endTime());
    }

    private Scope scope(DashboardQueryDTO dto) {
        Long teacherId = UserContext.isTeacher() ? UserContext.userId() : null;
        Long studentId = UserContext.isStudent() ? UserContext.userId() : null;
        return new Scope(teacherId, studentId);
    }

    private Period period(DashboardQueryDTO dto) {
        LocalDate startDate = dto.getStartDate() == null ? LocalDate.now().minusDays(29) : dto.getStartDate();
        LocalDate endDate = dto.getEndDate() == null ? LocalDate.now() : dto.getEndDate();
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(400, "结束日期不能早于开始日期");
        }
        return new Period(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    private Integer limit(DashboardQueryDTO dto) {
        return dto.getLimit() == null ? 10 : dto.getLimit();
    }

    private record Scope(Long teacherId, Long studentId) {}
    private record Period(LocalDateTime startTime, LocalDateTime endTime) {}
}
