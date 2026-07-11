package com.cupk.service.impl;

import com.cupk.dto.ai.AiAskDTO;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.interceptor.UserSession;
import com.cupk.mapper.*;
import com.cupk.pojo.CourseStudent;
import com.cupk.pojo.ExamAnswer;
import com.cupk.pojo.ExamPaper;
import com.cupk.pojo.ExamRecord;
import com.cupk.pojo.Experiment;
import com.cupk.service.ReportRubricService;
import com.cupk.service.ai.AiModelClient;
import com.cupk.service.ai.AiResponseParser;
import com.cupk.vo.ai.AiReportPrecheckVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiChatServiceImplTest {
    private ExamAnswerMapper examAnswerMapper;
    private ExamRecordMapper examRecordMapper;
    private ExamPaperMapper examPaperMapper;
    private CourseStudentMapper courseStudentMapper;
    private AiChatServiceImpl service;

    @BeforeEach
    void setUp() {
        examAnswerMapper = mock(ExamAnswerMapper.class);
        examRecordMapper = mock(ExamRecordMapper.class);
        examPaperMapper = mock(ExamPaperMapper.class);
        courseStudentMapper = mock(CourseStudentMapper.class);
        service = new AiChatServiceImpl(mock(AiChatRecordMapper.class), courseStudentMapper,
                mock(ExperimentMapper.class), mock(ExperimentStepMapper.class), mock(LabCourseMapper.class),
                mock(TeachingResourceMapper.class), examAnswerMapper, examRecordMapper, examPaperMapper,
                mock(QuestionMapper.class), mock(ReportRubricService.class), mock(AiModelClient.class), new AiResponseParser());
        UserContext.set(new UserSession(10L, List.of("USER"), List.of("ai:ask")));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void localReportPrecheckDetectsShortContentAndMissingSafetyReflection() {
        Experiment experiment = new Experiment();
        experiment.setExpName("测试实验");
        AiReportPrecheckVO result = AiChatServiceImpl.buildLocalReportPrecheck("实验目的：了解基本原理。结论：完成实验。", experiment);
        assertTrue(result.getFallback());
        assertEquals("NEEDS_IMPROVEMENT", result.getOverallStatus());
        assertTrue(result.getMissingItems().stream().anyMatch(item -> item.contains("正文篇幅较短")));
        assertTrue(result.getMissingItems().stream().anyMatch(item -> item.contains("安全风险")));
        assertFalse(result.getFabricationWarning().isBlank());
    }

    @Test
    void wrongAnswerDiagnosisRejectsOtherStudentRecord() {
        when(examAnswerMapper.selectById(1L)).thenReturn(answer(1L, 20L, 0));
        when(examRecordMapper.selectById(20L)).thenReturn(record(20L, 99L, "GRADED"));
        BusinessException error = assertThrows(BusinessException.class, () -> service.explainWrongAnswer(1L));
        assertEquals(403, error.getCode());
    }

    @Test
    void wrongAnswerDiagnosisRejectsInProgressRecord() {
        when(examAnswerMapper.selectById(1L)).thenReturn(answer(1L, 20L, 0));
        when(examRecordMapper.selectById(20L)).thenReturn(record(20L, 10L, "IN_PROGRESS"));
        BusinessException error = assertThrows(BusinessException.class, () -> service.explainWrongAnswer(1L));
        assertEquals(400, error.getCode());
    }

    @Test
    void wrongAnswerDiagnosisRejectsPaperThatHidesAnswers() {
        when(examAnswerMapper.selectById(1L)).thenReturn(answer(1L, 20L, 0));
        when(examRecordMapper.selectById(20L)).thenReturn(record(20L, 10L, "GRADED"));
        ExamPaper paper = new ExamPaper();
        paper.setId(30L);
        paper.setShowAnswerAfterSubmit(0);
        when(examPaperMapper.selectById(30L)).thenReturn(paper);
        BusinessException error = assertThrows(BusinessException.class, () -> service.explainWrongAnswer(1L));
        assertEquals(403, error.getCode());
    }

    @Test
    void questionRejectsCourseOutsideStudentScope() {
        CourseStudent enrollment = new CourseStudent();
        enrollment.setCourseId(1L);
        when(courseStudentMapper.selectList(any())).thenReturn(List.of(enrollment));
        AiAskDTO dto = new AiAskDTO();
        dto.setScene("SAFETY_QA");
        dto.setQuestion("安全要求是什么");
        dto.setCourseId(2L);
        BusinessException error = assertThrows(BusinessException.class, () -> service.ask(dto));
        assertEquals(403, error.getCode());
    }

    @Test
    void reportModelResponseAcceptsStringRewriteHints() {
        AiReportPrecheckVO result = service.parseReportModelResponse("""
                {"overallStatus":"NEEDS_IMPROVEMENT","summary":"需要完善",\
                "missingItems":[],"evidenceNeeded":[],"safetyQuestions":[],\
                "rewriteHints":["补充真实数据依据"]}
                """);

        assertEquals(1, result.getRewriteHints().size());
        assertEquals("写作建议", result.getRewriteHints().getFirst().getSection());
        assertEquals("补充真实数据依据", result.getRewriteHints().getFirst().getSuggestion());
    }

    private ExamAnswer answer(Long id, Long recordId, Integer isCorrect) {
        ExamAnswer answer = new ExamAnswer();
        answer.setId(id);
        answer.setRecordId(recordId);
        answer.setQuestionId(2L);
        answer.setIsCorrect(isCorrect);
        return answer;
    }

    private ExamRecord record(Long id, Long studentId, String status) {
        ExamRecord record = new ExamRecord();
        record.setId(id);
        record.setStudentId(studentId);
        record.setPaperId(30L);
        record.setStatus(status);
        return record;
    }
}
