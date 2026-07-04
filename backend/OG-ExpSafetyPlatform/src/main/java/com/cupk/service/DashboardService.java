package com.cupk.service;

import com.cupk.dto.DashboardQueryDTO;
import com.cupk.vo.CapacityUsageVO;
import com.cupk.vo.CourseCompletionVO;
import com.cupk.vo.DashboardOverviewVO;
import com.cupk.vo.ExamPassRateVO;
import com.cupk.vo.PieItemVO;
import com.cupk.vo.ResourceCompletionVO;
import com.cupk.vo.ResourceRankingVO;
import com.cupk.vo.TrendItemVO;
import com.cupk.vo.WrongKnowledgeRankingVO;

import java.util.List;

public interface DashboardService {
    DashboardOverviewVO overview(DashboardQueryDTO dto);
    List<CourseCompletionVO> courseCompletion(DashboardQueryDTO dto);
    List<PieItemVO> resourceTypeDistribution(DashboardQueryDTO dto);
    List<ResourceRankingVO> hotResources(DashboardQueryDTO dto);
    List<ResourceCompletionVO> resourceCompletion(DashboardQueryDTO dto);
    List<ExamPassRateVO> examPassRate(DashboardQueryDTO dto);
    List<WrongKnowledgeRankingVO> wrongKnowledgeRanking(DashboardQueryDTO dto);
    List<TrendItemVO> reservationTrend(DashboardQueryDTO dto);
    List<PieItemVO> reservationStatusDistribution(DashboardQueryDTO dto);
    List<CapacityUsageVO> capacityUsage(DashboardQueryDTO dto);
    List<PieItemVO> reportScoreDistribution(DashboardQueryDTO dto);
    List<TrendItemVO> learningActivityTrend(DashboardQueryDTO dto);
}
