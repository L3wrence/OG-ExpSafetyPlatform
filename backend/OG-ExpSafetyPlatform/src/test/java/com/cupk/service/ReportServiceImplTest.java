package com.cupk.service;

import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.interceptor.UserSession;
import com.cupk.mapper.CourseStudentMapper;
import com.cupk.mapper.ExperimentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.ReportMapper;
import com.cupk.mapper.ReportScoreMapper;
import com.cupk.mapper.ReservationMapper;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.Report;
import com.cupk.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @InjectMocks
    private ReportServiceImpl reportService;

    @Mock
    private ReportMapper reportMapper;
    @Mock
    private ReportScoreMapper reportScoreMapper;
    @Mock
    private ExperimentMapper experimentMapper;
    @Mock
    private LabCourseMapper labCourseMapper;
    @Mock
    private CourseStudentMapper courseStudentMapper;
    @Mock
    private ReservationMapper reservationMapper;

    @BeforeEach
    void setUp() {
        UserContext.set(new UserSession(10L, List.of("USER"), List.of("report:create")));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createReport_shouldRejectStudentOutsideCourse() {
        when(experimentMapper.selectById(2L)).thenReturn(experiment(false));
        when(labCourseMapper.selectById(3L)).thenReturn(course());
        when(courseStudentMapper.selectCount(any())).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class, () -> reportService.createReport(newReport()));

        assertEquals(403, ex.getCode());
        verify(reportMapper, never()).insert(any(Report.class));
    }

    @Test
    void createReport_shouldRejectWithoutApprovedReservation_whenReservationRequired() {
        when(experimentMapper.selectById(2L)).thenReturn(experiment(true));
        when(labCourseMapper.selectById(3L)).thenReturn(course());
        when(courseStudentMapper.selectCount(any())).thenReturn(1L);
        when(reservationMapper.selectCount(any())).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class, () -> reportService.createReport(newReport()));

        assertEquals(403, ex.getCode());
        verify(reportMapper, never()).insert(any(Report.class));
    }

    @Test
    void submitReport_shouldRequireContent() {
        Report report = newReport();
        report.setId(100L);
        report.setStudentId(10L);
        report.setStatus("DRAFT");
        report.setContent(" ");
        when(reportMapper.selectById(100L)).thenReturn(report);
        when(experimentMapper.selectById(2L)).thenReturn(experiment(false));
        when(labCourseMapper.selectById(3L)).thenReturn(course());
        when(courseStudentMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> reportService.submitReport(100L));

        assertEquals(400, ex.getCode());
        verify(reportMapper, never()).updateById(any(Report.class));
    }

    @Test
    void submitReport_shouldBeIdempotent_whenAlreadySubmitted() {
        Report report = newReport();
        report.setId(100L);
        report.setStudentId(10L);
        report.setStatus("SUBMITTED");
        when(reportMapper.selectById(100L)).thenReturn(report);

        reportService.submitReport(100L);

        verify(reportMapper, never()).updateById(any(Report.class));
    }

    @Test
    void uploadReportFile_shouldRejectUnsupportedExtension() {
        when(experimentMapper.selectById(2L)).thenReturn(experiment(false));
        when(labCourseMapper.selectById(3L)).thenReturn(course());
        when(courseStudentMapper.selectCount(any())).thenReturn(1L);
        MockMultipartFile file = new MockMultipartFile("file", "script.exe", "application/octet-stream", new byte[]{1});

        BusinessException ex = assertThrows(BusinessException.class, () -> reportService.uploadReportFile(2L, file));

        assertEquals(400, ex.getCode());
    }

    private Report newReport() {
        Report report = new Report();
        report.setExperimentId(2L);
        report.setTitle("实验报告");
        report.setContent("正文");
        return report;
    }

    private Experiment experiment(boolean reservationEnabled) {
        Experiment experiment = new Experiment();
        experiment.setId(2L);
        experiment.setCourseId(3L);
        experiment.setReservationEnabled(reservationEnabled ? 1 : 0);
        experiment.setStatus(1);
        return experiment;
    }

    private LabCourse course() {
        LabCourse course = new LabCourse();
        course.setId(3L);
        course.setTeacherId(20L);
        course.setStatus(1);
        return course;
    }
}
