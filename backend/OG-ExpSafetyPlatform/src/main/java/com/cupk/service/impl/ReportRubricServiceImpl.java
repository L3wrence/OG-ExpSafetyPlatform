package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cupk.dto.ReportRubricGradeDTO;
import com.cupk.dto.ReportRubricItemDTO;
import com.cupk.dto.ReportRubricScoreItemDTO;
import com.cupk.dto.ReportTemplateDTO;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.ExperimentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.CourseStudentMapper;
import com.cupk.mapper.ReportMapper;
import com.cupk.mapper.ReportRubricItemMapper;
import com.cupk.mapper.ReportScoreItemMapper;
import com.cupk.mapper.ReportScoreMapper;
import com.cupk.mapper.ReportTemplateMapper;
import com.cupk.pojo.CourseStudent;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.Report;
import com.cupk.pojo.ReportRubricItem;
import com.cupk.pojo.ReportScore;
import com.cupk.pojo.ReportScoreItem;
import com.cupk.pojo.ReportTemplate;
import com.cupk.service.PortalMessageService;
import com.cupk.service.ReportRubricService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportRubricServiceImpl implements ReportRubricService {
    private final ReportTemplateMapper templateMapper;
    private final ReportRubricItemMapper rubricItemMapper;
    private final ReportScoreItemMapper scoreItemMapper;
    private final ReportMapper reportMapper;
    private final ReportScoreMapper reportScoreMapper;
    private final ExperimentMapper experimentMapper;
    private final LabCourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final PortalMessageService messageService;

    public ReportRubricServiceImpl(ReportTemplateMapper templateMapper,
                                   ReportRubricItemMapper rubricItemMapper,
                                   ReportScoreItemMapper scoreItemMapper,
                                   ReportMapper reportMapper,
                                   ReportScoreMapper reportScoreMapper,
                                   ExperimentMapper experimentMapper,
                                   LabCourseMapper courseMapper,
                                   CourseStudentMapper courseStudentMapper,
                                   PortalMessageService messageService) {
        this.templateMapper = templateMapper;
        this.rubricItemMapper = rubricItemMapper;
        this.scoreItemMapper = scoreItemMapper;
        this.reportMapper = reportMapper;
        this.reportScoreMapper = reportScoreMapper;
        this.experimentMapper = experimentMapper;
        this.courseMapper = courseMapper;
        this.courseStudentMapper = courseStudentMapper;
        this.messageService = messageService;
    }

    @Override
    @Transactional
    public ReportTemplate saveTemplate(ReportTemplateDTO dto) {
        assertExperimentWritable(dto.getExperimentId());
        ReportTemplate template = template(dto.getExperimentId());
        if (template == null) {
            template = new ReportTemplate();
            template.setExperimentId(dto.getExperimentId());
        }
        template.setTitle(dto.getTitle());
        template.setSchemaJson(dto.getSchemaJson());
        template.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (template.getId() == null) {
            templateMapper.insert(template);
        } else {
            templateMapper.updateById(template);
        }
        return template;
    }

    @Override
    public ReportTemplate template(Long experimentId) {
        assertExperimentReadable(experimentId);
        return templateMapper.selectOne(new LambdaQueryWrapper<ReportTemplate>()
                .eq(ReportTemplate::getExperimentId, experimentId)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional
    public List<ReportRubricItem> saveRubric(Long experimentId, List<ReportRubricItemDTO> items) {
        assertExperimentWritable(experimentId);
        if (items == null || items.isEmpty()) {
            throw new BusinessException(400, "评分量规不能为空");
        }
        int totalMax = items.stream().mapToInt(ReportRubricItemDTO::getMaxScore).sum();
        if (totalMax <= 0) {
            throw new BusinessException(400, "评分项总分必须大于0");
        }
        rubricItemMapper.delete(new LambdaQueryWrapper<ReportRubricItem>()
                .eq(ReportRubricItem::getExperimentId, experimentId));
        int index = 1;
        for (ReportRubricItemDTO dto : items) {
            ReportRubricItem item = new ReportRubricItem();
            BeanUtils.copyProperties(dto, item);
            item.setId(null);
            item.setExperimentId(experimentId);
            item.setOrderNo(dto.getOrderNo() == null ? index : dto.getOrderNo());
            rubricItemMapper.insert(item);
            index++;
        }
        return rubric(experimentId);
    }

    @Override
    public List<ReportRubricItem> rubric(Long experimentId) {
        assertExperimentReadable(experimentId);
        return rubricItemMapper.selectList(new LambdaQueryWrapper<ReportRubricItem>()
                .eq(ReportRubricItem::getExperimentId, experimentId)
                .orderByAsc(ReportRubricItem::getOrderNo)
                .orderByAsc(ReportRubricItem::getId));
    }

    @Override
    @Transactional
    public void grade(Long reportId, ReportRubricGradeDTO dto) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(404, "报告不存在");
        }
        assertExperimentWritable(report.getExperimentId());
        if (!"SUBMITTED".equals(report.getStatus())) {
            throw new BusinessException(400, "只有已提交报告可以评分");
        }
        List<ReportRubricItem> rubricItems = rubric(report.getExperimentId());
        if (rubricItems.isEmpty()) {
            throw new BusinessException(400, "请先配置评分量规");
        }
        Map<Long, ReportRubricItem> rubricMap = rubricItems.stream()
                .collect(Collectors.toMap(ReportRubricItem::getId, Function.identity()));
        BigDecimal rawScore = BigDecimal.ZERO;
        for (ReportRubricScoreItemDTO itemDTO : dto.getItems()) {
            ReportRubricItem rubricItem = rubricMap.get(itemDTO.getRubricItemId());
            if (rubricItem == null) {
                throw new BusinessException(400, "评分项不属于当前实验");
            }
            BigDecimal maxScore = BigDecimal.valueOf(rubricItem.getMaxScore());
            if (itemDTO.getScore().compareTo(maxScore) > 0) {
                throw new BusinessException(400, "评分不能超过评分项满分");
            }
            rawScore = rawScore.add(itemDTO.getScore());
        }
        BigDecimal maxTotal = rubricItems.stream()
                .map(item -> BigDecimal.valueOf(item.getMaxScore()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int finalScore = rawScore.multiply(BigDecimal.valueOf(100))
                .divide(maxTotal, 0, RoundingMode.HALF_UP)
                .intValue();

        reportScoreMapper.update(null, new LambdaUpdateWrapper<ReportScore>()
                .eq(ReportScore::getReportId, reportId)
                .eq(ReportScore::getIsLatest, 1)
                .set(ReportScore::getIsLatest, 0));
        ReportScore score = new ReportScore();
        score.setReportId(reportId);
        score.setTeacherId(UserContext.userId());
        score.setScore(finalScore);
        score.setComment(dto.getComment());
        score.setIsLatest(1);
        score.setCreateTime(new java.util.Date());
        score.setGradeTime(new java.util.Date());
        reportScoreMapper.insert(score);
        for (ReportRubricScoreItemDTO itemDTO : dto.getItems()) {
            ReportScoreItem scoreItem = new ReportScoreItem();
            scoreItem.setReportScoreId(score.getId());
            scoreItem.setRubricItemId(itemDTO.getRubricItemId());
            scoreItem.setScore(itemDTO.getScore());
            scoreItem.setComment(itemDTO.getComment());
            scoreItemMapper.insert(scoreItem);
        }
        Report update = new Report();
        update.setId(reportId);
        update.setStatus("GRADED");
        reportMapper.updateById(update);
        messageService.send(report.getStudentId(), "报告评分完成", report.getTitle(),
                "REPORT_GRADED", report.getId(), "/student/grades");
    }

    @Override
    public Map<String, Object> scoreItems(Long reportScoreId) {
        Map<String, Object> result = new HashMap<>();
        result.put("items", scoreItemMapper.selectList(new LambdaQueryWrapper<ReportScoreItem>()
                .eq(ReportScoreItem::getReportScoreId, reportScoreId)
                .orderByAsc(ReportScoreItem::getRubricItemId)));
        return result;
    }

    private void assertExperimentReadable(Long experimentId) {
        Experiment experiment = requireExperiment(experimentId);
        LabCourse course = requireCourse(experiment.getCourseId());
        if (UserContext.isAdmin()) {
            return;
        }
        if (UserContext.isTeacher()) {
            if (!UserContext.userId().equals(course.getTeacherId())) {
                throw new BusinessException(403, "不能访问非本人负责课程的报告配置");
            }
            return;
        }
        if (UserContext.isStudent()) {
            Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, course.getId())
                    .eq(CourseStudent::getStudentId, UserContext.userId())
                    .eq(CourseStudent::getStatus, 1)
                    .eq(CourseStudent::getDeleted, 0));
            if (count == null || count == 0) {
                throw new BusinessException(403, "不能查看未选课程的报告要求");
            }
            return;
        }
        throw new BusinessException(403, "无权访问报告配置");
    }

    private void assertExperimentWritable(Long experimentId) {
        Experiment experiment = requireExperiment(experimentId);
        LabCourse course = requireCourse(experiment.getCourseId());
        if (UserContext.isAdmin()) {
            return;
        }
        if (UserContext.isTeacher() && UserContext.userId().equals(course.getTeacherId())) {
            return;
        }
        throw new BusinessException(403, "不能维护非本人负责课程的报告配置");
    }

    private Experiment requireExperiment(Long id) {
        Experiment experiment = experimentMapper.selectById(id);
        if (experiment == null) {
            throw new BusinessException(404, "实验不存在");
        }
        return experiment;
    }

    private LabCourse requireCourse(Long id) {
        LabCourse course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        return course;
    }
}
