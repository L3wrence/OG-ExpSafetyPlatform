package com.cupk.service;

import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.interceptor.UserSession;
import com.cupk.mapper.CourseStudentMapper;
import com.cupk.mapper.ExamAnswerMapper;
import com.cupk.mapper.ExamPaperMapper;
import com.cupk.mapper.ExamPaperQuestionMapper;
import com.cupk.mapper.ExamRecordMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.QuestionMapper;
import com.cupk.pojo.ExamAnswer;
import com.cupk.pojo.ExamPaper;
import com.cupk.pojo.ExamRecord;
import com.cupk.pojo.ExperimentAdmission;
import com.cupk.service.impl.ExamServiceImpl;
import com.cupk.vo.ExamSessionVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamServiceImplTest {

    @InjectMocks
    private ExamServiceImpl examService;

    @Mock
    private ExamPaperMapper examPaperMapper;
    @Mock
    private ExamPaperQuestionMapper examPaperQuestionMapper;
    @Mock
    private ExamRecordMapper examRecordMapper;
    @Mock
    private ExamAnswerMapper examAnswerMapper;
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private AdmissionService admissionService;
    @Mock
    private LearningTaskService learningTaskService;
    @Mock
    private CourseStudentMapper courseStudentMapper;
    @Mock
    private LabCourseMapper courseMapper;

    @BeforeEach
    void setUp() {
        UserContext.set(new UserSession(10L, List.of("USER"), List.of("exam:take")));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void startExam_shouldResumeExistingRecord_withoutIncreasingAttemptCount() {
        ExamPaper paper = paper();
        ExamRecord record = inProgressRecord();
        record.setQuestionSnapshotJson(singleSnapshot());
        ExamAnswer saved = answer(100L, 1L, "B", 0);

        when(examPaperMapper.selectById(1L)).thenReturn(paper);
        when(courseStudentMapper.selectCount(any())).thenReturn(1L);
        when(examRecordMapper.selectOne(any())).thenReturn(record);
        when(examAnswerMapper.selectList(any())).thenReturn(List.of(saved));

        ExamSessionVO session = examService.startExam(1L);

        assertTrue(session.getResumed());
        assertEquals(100L, session.getRecordId());
        assertEquals("B", session.getAnswers().get(1L));
        verify(examRecordMapper, never()).insert(any(ExamRecord.class));
    }

    @Test
    void startExam_shouldRejectStudentOutsideCourse() {
        when(examPaperMapper.selectById(1L)).thenReturn(paper());
        when(courseStudentMapper.selectCount(any())).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class, () -> examService.startExam(1L));

        assertEquals(403, ex.getCode());
        verify(examRecordMapper, never()).insert(any(ExamRecord.class));
    }

    @Test
    void submitExam_shouldNormalizeJudgeHistoryAnswer() {
        ExamRecord record = inProgressRecord();
        record.setQuestionSnapshotJson(judgeSnapshot());
        ExamPaper paper = paper();

        when(examRecordMapper.selectById(100L)).thenReturn(record);
        when(examPaperMapper.selectById(1L)).thenReturn(paper);
        when(examAnswerMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = examService.submitExam(100L, List.of(Map.of("questionId", 1L, "answer", "A")));

        assertEquals(5, result.get("totalScore"));
        assertEquals(Boolean.FALSE, result.get("autoSubmit"));
        verify(admissionService).issueOnPassedExam(eq(record), eq(paper));
    }

    @Test
    void submitExam_shouldNotGrantAdmission_whenShortAnswerPendingReview() {
        ExamRecord record = inProgressRecord();
        record.setQuestionSnapshotJson(shortAnswerSnapshot());

        when(examRecordMapper.selectById(100L)).thenReturn(record);
        when(examPaperMapper.selectById(1L)).thenReturn(paper());
        when(examAnswerMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = examService.submitExam(100L, List.of(Map.of("questionId", 2L, "answer", "主观答案")));

        assertEquals("PENDING_REVIEW", result.get("status"));
        assertNull(result.get("passed"));
        verify(admissionService, never()).issueOnPassedExam(any(), any());
    }

    @Test
    void autoSubmit_shouldRemainPendingReview_whenPaperHasShortAnswer() {
        ExamRecord record = inProgressRecord();
        record.setQuestionSnapshotJson(shortAnswerSnapshot());

        when(examRecordMapper.selectById(100L)).thenReturn(record);
        when(examPaperMapper.selectById(1L)).thenReturn(paper());
        when(examAnswerMapper.selectList(any())).thenReturn(List.of());

        Map<String, Object> result = examService.submitExam(
                100L, List.of(Map.of("questionId", 2L, "answer", "主观答案")), true);

        assertEquals("PENDING_REVIEW", result.get("status"));
        assertNull(result.get("passed"));
        assertEquals(Boolean.TRUE, result.get("autoSubmit"));
        verify(admissionService, never()).issueOnPassedExam(any(), any());
    }

    @Test
    void finalReview_shouldGrantAdmission_whenFinalScorePasses() {
        UserContext.set(new UserSession(20L, List.of("ADMIN"), List.of("exam:statistics")));
        ExamRecord record = inProgressRecord();
        record.setStatus("PENDING_REVIEW");
        record.setObjectiveScore(50);
        record.setQuestionSnapshotJson(shortAnswerSnapshot());
        ExamAnswer graded = answer(200L, 2L, "主观答案", 20);
        graded.setIsCorrect(1);
        ExperimentAdmission admission = new ExperimentAdmission();
        admission.setId(300L);

        when(examRecordMapper.selectById(100L)).thenReturn(record);
        when(examPaperMapper.selectById(1L)).thenReturn(paper());
        when(examAnswerMapper.selectById(200L)).thenReturn(graded);
        when(examAnswerMapper.selectCount(any())).thenReturn(0L);
        when(examAnswerMapper.selectList(any())).thenReturn(List.of(graded));
        when(admissionService.issueOnPassedExam(any(), any())).thenReturn(admission);

        Map<String, Object> result = examService.gradeShortAnswer(100L, List.of(Map.of("answerId", 200L, "score", 20)));

        assertEquals("GRADED", result.get("status"));
        assertEquals(Boolean.TRUE, result.get("passed"));
        verify(admissionService).issueOnPassedExam(eq(record), any());
        verify(learningTaskService).syncExamCompleted(10L, 1L, 2L);
    }

    private ExamPaper paper() {
        ExamPaper paper = new ExamPaper();
        paper.setId(1L);
        paper.setTitle("安全考试");
        paper.setCourseId(3L);
        paper.setExperimentId(2L);
        paper.setStatus("PUBLISHED");
        paper.setDuration(30);
        paper.setPassScore(5);
        paper.setShowAnswerAfterSubmit(1);
        return paper;
    }

    private ExamRecord inProgressRecord() {
        ExamRecord record = new ExamRecord();
        record.setId(100L);
        record.setStudentId(10L);
        record.setPaperId(1L);
        record.setExperimentId(2L);
        record.setStatus("IN_PROGRESS");
        record.setStartTime(new Date());
        record.setEndTime(new Date(System.currentTimeMillis() + 1_800_000L));
        return record;
    }

    private ExamAnswer answer(Long id, Long questionId, String value, int score) {
        ExamAnswer answer = new ExamAnswer();
        answer.setId(id);
        answer.setRecordId(100L);
        answer.setQuestionId(questionId);
        answer.setStudentAnswer(value);
        answer.setScore(score);
        return answer;
    }

    private String singleSnapshot() {
        return """
                [{"id":1,"type":"SINGLE","content":"题目","options":"{\\"A\\":\\"甲\\",\\"B\\":\\"乙\\"}","answer":"A","score":5,"orderNum":1}]
                """;
    }

    private String judgeSnapshot() {
        return """
                [{"id":1,"type":"JUDGE","content":"判断","options":null,"answer":"TRUE","score":5,"orderNum":1}]
                """;
    }

    private String shortAnswerSnapshot() {
        return """
                [{"id":2,"type":"SHORT_ANSWER","content":"简答","options":null,"answer":"要点","score":20,"orderNum":1}]
                """;
    }
}
