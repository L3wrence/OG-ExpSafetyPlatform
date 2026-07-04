package com.cupk.controller;

import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.dto.DashboardQueryDTO;
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
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    @RequirePermission("dashboard:view")
    public Result<DashboardOverviewVO> overview(@Valid DashboardQueryDTO dto) {
        return Result.success(dashboardService.overview(dto));
    }

    @GetMapping("/course-completion")
    @RequirePermission("dashboard:view")
    public Result<List<CourseCompletionVO>> courseCompletion(@Valid DashboardQueryDTO dto) {
        return Result.success(dashboardService.courseCompletion(dto));
    }

    @GetMapping("/resources/type-distribution")
    @RequirePermission("dashboard:view")
    public Result<List<PieItemVO>> resourceTypeDistribution(@Valid DashboardQueryDTO dto) {
        return Result.success(dashboardService.resourceTypeDistribution(dto));
    }

    @GetMapping("/resources/hot-ranking")
    @RequirePermission("dashboard:view")
    public Result<List<ResourceRankingVO>> hotResources(@Valid DashboardQueryDTO dto) {
        return Result.success(dashboardService.hotResources(dto));
    }

    @GetMapping("/resources/completion")
    @RequirePermission("dashboard:view")
    public Result<List<ResourceCompletionVO>> resourceCompletion(@Valid DashboardQueryDTO dto) {
        return Result.success(dashboardService.resourceCompletion(dto));
    }

    @GetMapping("/exams/pass-rate")
    @RequirePermission("dashboard:view")
    public Result<List<ExamPassRateVO>> examPassRate(@Valid DashboardQueryDTO dto) {
        return Result.success(dashboardService.examPassRate(dto));
    }

    @GetMapping("/exams/wrong-knowledge-ranking")
    @RequirePermission("dashboard:view")
    public Result<List<WrongKnowledgeRankingVO>> wrongKnowledgeRanking(@Valid DashboardQueryDTO dto) {
        return Result.success(dashboardService.wrongKnowledgeRanking(dto));
    }

    @GetMapping("/reservations/trend")
    @RequirePermission("dashboard:view")
    public Result<List<TrendItemVO>> reservationTrend(@Valid DashboardQueryDTO dto) {
        return Result.success(dashboardService.reservationTrend(dto));
    }

    @GetMapping("/reservations/status-distribution")
    @RequirePermission("dashboard:view")
    public Result<List<PieItemVO>> reservationStatusDistribution(@Valid DashboardQueryDTO dto) {
        return Result.success(dashboardService.reservationStatusDistribution(dto));
    }

    @GetMapping("/reservations/capacity-usage")
    @RequirePermission("dashboard:view")
    public Result<List<CapacityUsageVO>> capacityUsage(@Valid DashboardQueryDTO dto) {
        return Result.success(dashboardService.capacityUsage(dto));
    }

    @GetMapping("/reports/score-distribution")
    @RequirePermission("dashboard:view")
    public Result<List<PieItemVO>> reportScoreDistribution(@Valid DashboardQueryDTO dto) {
        return Result.success(dashboardService.reportScoreDistribution(dto));
    }

    @GetMapping("/learning/activity-trend")
    @RequirePermission("dashboard:view")
    public Result<List<TrendItemVO>> learningActivityTrend(@Valid DashboardQueryDTO dto) {
        return Result.success(dashboardService.learningActivityTrend(dto));
    }
}
