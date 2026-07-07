package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.common.PageResult;
import com.cupk.dto.ExperimentCreateDTO;
import com.cupk.dto.ExperimentQueryDTO;
import com.cupk.dto.ExperimentStepDTO;
import com.cupk.dto.ExperimentUpdateDTO;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.ExperimentStep;
import com.cupk.pojo.ExamPaper;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.OperationLog;
import com.cupk.pojo.Report;
import com.cupk.pojo.Reservation;
import com.cupk.pojo.SafetyKnowledge;
import com.cupk.pojo.TeachingResource;
import com.cupk.pojo.User;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.*;
import com.cupk.service.AdmissionService;
import com.cupk.service.ExperimentService;
import com.cupk.service.LearningRecordService;
import com.cupk.util.AccessUtil;
import com.cupk.vo.ExperimentDetailVO;
import com.cupk.vo.PortalItemVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ExperimentServiceImpl implements ExperimentService {
    private static final int STATUS_CLOSED = 0;
    private static final int STATUS_OPEN = 1;
    private static final int STATUS_MAINTENANCE = 2;

    private final ExperimentMapper experimentMapper;
    private final LabCourseMapper courseMapper;
    private final ExperimentStepMapper stepMapper;
    private final TeachingResourceMapper resourceMapper;
    private final SafetyKnowledgeMapper knowledgeMapper;
    private final BusinessReferenceMapper businessReferenceMapper;
    private final LearningRecordService learningRecordService;
    private final ExamRecordMapper examRecordMapper;
    private final ExamPaperMapper examPaperMapper;
    private final ReservationMapper reservationMapper;
    private final ReportMapper reportMapper;
    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;
    private final AdmissionService admissionService;

    public ExperimentServiceImpl(ExperimentMapper experimentMapper, LabCourseMapper courseMapper,
                                 ExperimentStepMapper stepMapper, TeachingResourceMapper resourceMapper,
                                 SafetyKnowledgeMapper knowledgeMapper, BusinessReferenceMapper businessReferenceMapper,
                                 LearningRecordService learningRecordService, ExamRecordMapper examRecordMapper,
                                 ExamPaperMapper examPaperMapper, ReservationMapper reservationMapper,
                                 ReportMapper reportMapper, OperationLogMapper operationLogMapper, UserMapper userMapper,
                                 AdmissionService admissionService) {
        this.experimentMapper = experimentMapper;
        this.courseMapper = courseMapper;
        this.stepMapper = stepMapper;
        this.resourceMapper = resourceMapper;
        this.knowledgeMapper = knowledgeMapper;
        this.businessReferenceMapper = businessReferenceMapper;
        this.learningRecordService = learningRecordService;
        this.examRecordMapper = examRecordMapper;
        this.examPaperMapper = examPaperMapper;
        this.reservationMapper = reservationMapper;
        this.reportMapper = reportMapper;
        this.operationLogMapper = operationLogMapper;
        this.userMapper = userMapper;
        this.admissionService = admissionService;
    }

    @Override
    @Transactional
    public Long create(ExperimentCreateDTO dto) {
        LabCourse course = requireCourse(dto.getCourseId());
        AccessUtil.assertCourseWritable(course);
        checkCode(dto.getCourseId(), dto.getExpCode(), null);
        validateRisk(dto.getRiskLevel());
        validateStatus(dto.getStatus());
        Experiment entity = new Experiment();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(dto.getStatus() == null ? STATUS_CLOSED : dto.getStatus());
        fillDirectionFromCourse(entity, course);
        if (entity.getStatus() == STATUS_OPEN) {
            validateOpenReady(entity, 0L);
        }
        experimentMapper.insert(entity);
        log("CREATE", "创建实验项目：" + entity.getExpName(), "SUCCESS");
        return entity.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ExperimentUpdateDTO dto) {
        Experiment current = requireExperiment(id);
        LabCourse oldCourse = requireCourse(current.getCourseId());
        AccessUtil.assertCourseWritable(oldCourse);
        LabCourse targetCourse = requireCourse(dto.getCourseId());
        AccessUtil.assertCourseWritable(targetCourse);
        checkCode(dto.getCourseId(), dto.getExpCode(), id);
        validateRisk(dto.getRiskLevel());
        validateStatus(dto.getStatus());
        BeanUtils.copyProperties(dto, current);
        fillDirectionFromCourse(current, targetCourse);
        if (current.getStatus() != null && current.getStatus() == STATUS_OPEN) {
            validateOpenReady(current, countSteps(id));
        }
        experimentMapper.updateById(current);
        log("UPDATE", "修改实验项目：" + current.getExpName(), "SUCCESS");
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Experiment entity = requireExperiment(id);
        AccessUtil.assertCourseWritable(requireCourse(entity.getCourseId()));
        if (businessReferenceMapper.countExperimentHistory(id) > 0) {
            throw new BusinessException(409, "实验已有考试、预约或报告历史，只能关闭，不能删除");
        }
        stepMapper.delete(new LambdaQueryWrapper<ExperimentStep>().eq(ExperimentStep::getExperimentId, id));
        resourceMapper.delete(new LambdaQueryWrapper<TeachingResource>().eq(TeachingResource::getExperimentId, id));
        knowledgeMapper.delete(new LambdaQueryWrapper<SafetyKnowledge>().eq(SafetyKnowledge::getExperimentId, id));
        experimentMapper.deleteById(id);
        log("DELETE", "删除实验项目：" + entity.getExpName(), "SUCCESS");
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        validateStatus(status);
        Experiment entity = requireExperiment(id);
        AccessUtil.assertCourseWritable(requireCourse(entity.getCourseId()));
        if (status == STATUS_OPEN) {
            validateOpenReady(entity, countSteps(id));
        }
        entity.setStatus(status);
        experimentMapper.updateById(entity);
        log("STATUS", "实验状态更新：" + entity.getExpName() + " -> " + status, "SUCCESS");
    }

    @Override
    @Transactional
    public void saveSteps(Long id, List<ExperimentStepDTO> steps) {
        Experiment experiment = requireExperiment(id);
        AccessUtil.assertCourseWritable(requireCourse(experiment.getCourseId()));
        if (steps == null) {
            throw new BusinessException(400, "步骤列表不能为空");
        }
        Set<Integer> stepNos = new HashSet<>();
        for (ExperimentStepDTO step : steps) {
            if (!stepNos.add(step.getStepNo())) {
                throw new BusinessException(400, "步骤序号不能重复");
            }
            validateStepMedia(step);
        }
        stepMapper.delete(new LambdaQueryWrapper<ExperimentStep>().eq(ExperimentStep::getExperimentId, id));
        for (ExperimentStepDTO dto : steps) {
            ExperimentStep entity = new ExperimentStep();
            BeanUtils.copyProperties(dto, entity);
            entity.setExperimentId(id);
            stepMapper.insert(entity);
        }
        if (experiment.getStatus() != null && experiment.getStatus() == STATUS_OPEN) {
            validateOpenReady(experiment, (long) steps.size());
        }
        log("STEP_SAVE", "保存实验步骤：" + experiment.getExpName() + "，共" + steps.size() + "步", "SUCCESS");
    }

    @Override
    public PageResult<Experiment> page(ExperimentQueryDTO dto) {
        LambdaQueryWrapper<Experiment> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(dto.getKeyword()), w -> w.like(Experiment::getExpName, dto.getKeyword())
                .or().like(Experiment::getExpCode, dto.getKeyword()));
        wrapper.eq(dto.getCourseId() != null, Experiment::getCourseId, dto.getCourseId());
        wrapper.eq(StringUtils.hasText(dto.getRiskLevel()), Experiment::getRiskLevel, dto.getRiskLevel());
        wrapper.eq(dto.getStatus() != null, Experiment::getStatus, dto.getStatus());
        if (UserContext.isStudent()) {
            wrapper.eq(Experiment::getStatus, STATUS_OPEN);
        } else if (UserContext.isTeacher()) {
            List<Long> courseIds = courseMapper.selectList(new LambdaQueryWrapper<LabCourse>()
                    .select(LabCourse::getId).eq(LabCourse::getTeacherId, UserContext.userId()))
                    .stream().map(LabCourse::getId).toList();
            if (courseIds.isEmpty()) return new PageResult<>(Collections.emptyList(), 0L, dto.getPageNum(), dto.getPageSize());
            wrapper.in(Experiment::getCourseId, courseIds);
        }
        wrapper.orderByAsc(Experiment::getSort).orderByDesc(Experiment::getCreateTime);
        Page<Experiment> page = experimentMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public ExperimentDetailVO detail(Long id) {
        Experiment experiment = requireExperiment(id);
        LabCourse course = requireCourse(experiment.getCourseId());
        if (UserContext.isStudent() && (experiment.getStatus() != STATUS_OPEN || course.getStatus() != 1)) {
            throw new BusinessException(403, "实验或课程未开放");
        }
        if (UserContext.isTeacher()) AccessUtil.assertCourseWritable(course);

        ExperimentDetailVO vo = new ExperimentDetailVO();
        vo.setExperiment(experiment);
        vo.setCourseName(course.getCourseName());
        vo.setSteps(stepMapper.selectList(new LambdaQueryWrapper<ExperimentStep>()
                .eq(ExperimentStep::getExperimentId, id).orderByAsc(ExperimentStep::getStepNo)));
        vo.setResources(resourceMapper.selectList(new LambdaQueryWrapper<TeachingResource>()
                .eq(TeachingResource::getExperimentId, id)
                .eq(UserContext.isStudent(), TeachingResource::getStatus, 1)
                .orderByAsc(TeachingResource::getSort).orderByAsc(TeachingResource::getId)));
        vo.setSafetyKnowledge(knowledgeMapper.selectList(new LambdaQueryWrapper<SafetyKnowledge>()
                .eq(SafetyKnowledge::getExperimentId, id)
                .eq(UserContext.isStudent(), SafetyKnowledge::getStatus, 1)
                .orderByAsc(SafetyKnowledge::getId)));

        if (UserContext.isStudent()) {
            BigDecimal progress = learningRecordService.experimentProgress(id, UserContext.userId()).getProgress();
            boolean passed = examRecordMapper.countPassed(UserContext.userId(), id) > 0;
            var admissionStatus = admissionService.getAdmissionStatus(UserContext.userId(), id);
            vo.setLearningProgress(progress);
            vo.setExamPassed(passed);
            vo.setAdmissionStatus(admissionStatus);
            vo.setReservationAllowed(experiment.getStatus() == STATUS_OPEN
                    && Integer.valueOf(1).equals(experiment.getReservationEnabled())
                    && Boolean.TRUE.equals(admissionStatus.get("qualified")));
        } else {
            vo.setLearningProgress(BigDecimal.ZERO);
            vo.setExamPassed(false);
            vo.setReservationAllowed(false);
        }
        vo.setExamCount(examPaperMapper.selectCount(new LambdaQueryWrapper<ExamPaper>().eq(ExamPaper::getExperimentId, id)));
        vo.setReservationCount(reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>().eq(Reservation::getExperimentId, id)));
        vo.setReportCount(reportMapper.selectCount(new LambdaQueryWrapper<Report>().eq(Report::getExperimentId, id)));
        vo.setEntrances(buildEntrances(id));
        return vo;
    }

    private void checkCode(Long courseId, String code, Long excludeId) {
        long count = experimentMapper.selectCount(new LambdaQueryWrapper<Experiment>()
                .eq(Experiment::getCourseId, courseId)
                .eq(Experiment::getExpCode, code)
                .ne(excludeId != null, Experiment::getId, excludeId));
        if (count > 0) throw new BusinessException(409, "同一课程中的实验编号不能重复");
    }

    private void validateRisk(String risk) {
        if (!List.of("LOW", "MEDIUM", "HIGH").contains(risk)) {
            throw new BusinessException(400, "风险等级必须是LOW、MEDIUM或HIGH");
        }
    }

    private void validateStatus(Integer status) {
        if (status == null) {
            return;
        }
        if (status != STATUS_CLOSED && status != STATUS_OPEN && status != STATUS_MAINTENANCE) {
            throw new BusinessException(400, "状态只能为0、1或2");
        }
    }

    private void validateOpenReady(Experiment experiment, Long stepCount) {
        if (!StringUtils.hasText(experiment.getExpName())
                || experiment.getCourseId() == null
                || !StringUtils.hasText(experiment.getObjective())
                || !StringUtils.hasText(experiment.getPrinciple())
                || !StringUtils.hasText(experiment.getEquipment())
                || !StringUtils.hasText(experiment.getRiskLevel())
                || experiment.getDurationMinutes() == null
                || experiment.getDurationMinutes() <= 0) {
            throw new BusinessException(400, "开放实验前需完善名称、课程、目标、原理、仪器设备、风险等级和时长");
        }
        if (stepCount == null || stepCount <= 0) {
            throw new BusinessException(400, "开放实验前至少需要配置一个实验步骤");
        }
        if (!StringUtils.hasText(experiment.getSafetyRequirement())) {
            throw new BusinessException(400, "开放实验前需配置安全要求");
        }
        if ("HIGH".equals(experiment.getRiskLevel())) {
            if (!StringUtils.hasText(experiment.getPpeRequirements())) {
                throw new BusinessException(400, "高风险实验必须配置PPE个人防护用品要求");
            }
            if (!Integer.valueOf(1).equals(experiment.getExamRequired())
                    || experiment.getSafetyPassScore() == null
                    || experiment.getSafetyPassScore() <= 0) {
                throw new BusinessException(400, "高风险实验必须配置准入考试和最低通过分数");
            }
        }
    }

    private void validateStepMedia(ExperimentStepDTO step) {
        if (!StringUtils.hasText(step.getMediaType())) {
            return;
        }
        if (!List.of("TEXT", "IMAGE", "VIDEO", "FLOWCHART").contains(step.getMediaType())) {
            throw new BusinessException(400, "步骤媒体类型必须是TEXT、IMAGE、VIDEO或FLOWCHART");
        }
        if (List.of("IMAGE", "VIDEO").contains(step.getMediaType()) && !StringUtils.hasText(step.getMediaUrl())) {
            throw new BusinessException(400, "图片或视频步骤必须提供媒体地址");
        }
        if ("FLOWCHART".equals(step.getMediaType()) && !StringUtils.hasText(step.getFlowchartData())) {
            throw new BusinessException(400, "流程图步骤必须提供流程图数据");
        }
    }

    private Long countSteps(Long experimentId) {
        return stepMapper.selectCount(new LambdaQueryWrapper<ExperimentStep>().eq(ExperimentStep::getExperimentId, experimentId));
    }

    private void fillDirectionFromCourse(Experiment experiment, LabCourse course) {
        if (!StringUtils.hasText(experiment.getDirection())) {
            experiment.setDirection(course.getDirection());
        }
    }

    private List<PortalItemVO> buildEntrances(Long experimentId) {
        PortalItemVO resources = entrance("教学资源", "resource", "/teacher/resources?experimentId=" + experimentId);
        PortalItemVO exams = entrance("安全考试", "exam", "/teacher/exam-papers?experimentId=" + experimentId);
        PortalItemVO reservations = entrance("实验预约", "reservation", "/teacher/reservations?experimentId=" + experimentId);
        PortalItemVO reports = entrance("实验报告", "report", "/teacher/reports?experimentId=" + experimentId);
        return List.of(resources, exams, reservations, reports);
    }

    private PortalItemVO entrance(String title, String type, String path) {
        PortalItemVO item = new PortalItemVO();
        item.setTitle(title);
        item.setType(type);
        item.setPath(path);
        return item;
    }

    private void log(String action, String content, String result) {
        OperationLog log = new OperationLog();
        log.setUserId(UserContext.userId());
        User user = userMapper.selectById(UserContext.userId());
        log.setUsername(user == null ? null : user.getUsername());
        log.setModule("EXPERIMENT_PROCEDURE");
        log.setAction(action);
        log.setContent(content);
        log.setResult(result);
        operationLogMapper.insert(log);
    }

    private Experiment requireExperiment(Long id) {
        Experiment entity = experimentMapper.selectById(id);
        if (entity == null) throw new BusinessException(404, "实验项目不存在");
        return entity;
    }

    private LabCourse requireCourse(Long id) {
        LabCourse course = courseMapper.selectById(id);
        if (course == null) throw new BusinessException(404, "课程不存在");
        return course;
    }
}
