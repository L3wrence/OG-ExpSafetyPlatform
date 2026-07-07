package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.mapper.*;
import com.cupk.pojo.*;
import com.cupk.service.AdmissionService;
import com.cupk.service.PortalMessageService;
import com.cupk.service.ReservationService;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.vo.ReservationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 瀹為獙棰勭害鏈嶅姟瀹炵幇锛堟牳蹇冿細鍚祫鏍兼牎楠?骞跺彂鎺у埗锛?
 */
@Service
public class ReservationServiceImpl implements ReservationService {

    @Autowired
    private LabTimeSlotMapper labTimeSlotMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private LabCourseMapper labCourseMapper;

    @Autowired
    private ExperimentMapper experimentMapper;

    @Autowired
    private AdmissionService admissionService;

    @Autowired
    private PortalMessageService portalMessageService;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Page<LabTimeSlot> pageTimeSlots(int pageNum, int pageSize, Long labId, String date, String status) {
        Page<LabTimeSlot> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LabTimeSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(labId != null, LabTimeSlot::getLabId, labId)
               .eq(date != null && !date.isEmpty(), LabTimeSlot::getDate, date)
               .eq(status != null && !status.isEmpty(), LabTimeSlot::getStatus, status)
               .orderByDesc(LabTimeSlot::getDate)
               .orderByAsc(LabTimeSlot::getStartTime);
        if (UserContext.isTeacher()) {
            List<Long> experimentIds = teacherExperimentIds();
            if (experimentIds.isEmpty()) {
                page.setRecords(new ArrayList<>());
                page.setTotal(0);
                return page;
            }
            wrapper.in(LabTimeSlot::getExperimentId, experimentIds);
        }
        return labTimeSlotMapper.selectPage(page, wrapper);
    }

    @Override
    public Map<String, Integer> batchCreateTimeSlots(List<LabTimeSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            throw new BusinessException(400, "时间段列表不能为空");
        }
        int successCount = 0;
        int failCount = 0;
        for (LabTimeSlot slot : slots) {
            try {
                if (slot.getLabId() == null || slot.getDate() == null || slot.getStartTime() == null
                        || slot.getEndTime() == null || slot.getCapacity() == null || slot.getCapacity() <= 0) {
                    throw new BusinessException(400, "时间段信息不完整");
                }
                assertExperimentReservable(slot.getExperimentId());
                assertExperimentWritable(slot.getExperimentId());
                slot.setBookedCount(0);
                slot.setStatus("AVAILABLE");
                slot.setCreateBy(UserContext.getUserId());
                slot.setCreateTime(new java.util.Date());
                labTimeSlotMapper.insert(slot);
                successCount++;
            } catch (Exception e) {
                failCount++;
            }
        }
        Map<String, Integer> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        return result;
    }

    @Override
    public void updateTimeSlot(Long id, LabTimeSlot slot) {
        LabTimeSlot current = labTimeSlotMapper.selectById(id);
        if (current == null) {
            throw new BusinessException(404, "时间段不存在");
        }
        Long experimentId = slot.getExperimentId() != null ? slot.getExperimentId() : (current == null ? null : current.getExperimentId());
        assertExperimentReservable(experimentId);
        assertExperimentWritable(experimentId);
        slot.setId(id);
        labTimeSlotMapper.updateById(slot);
    }

    @Override
    public void deleteTimeSlot(Long id) {
        LabTimeSlot current = labTimeSlotMapper.selectById(id);
        if (current == null) {
            throw new BusinessException(404, "时间段不存在");
        }
        assertExperimentWritable(current.getExperimentId());
        Long activeReservations = reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getTimeSlotId, id)
                .in(Reservation::getStatus, "PENDING", "APPROVED"));
        if (activeReservations > 0) {
            throw new BusinessException(409, "该时间段已有预约记录，不能删除");
        }
        labTimeSlotMapper.deleteById(id);
    }

    @Override
    public Page<Map<String, Object>> getAvailableSlots(Long labId, String date, int pageNum, int pageSize) {
        Page<LabTimeSlot> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LabTimeSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(labId != null, LabTimeSlot::getLabId, labId)
               .eq(date != null && !date.isEmpty(), LabTimeSlot::getDate, date)
               .eq(LabTimeSlot::getStatus, "AVAILABLE")
               .apply("booked_count < capacity")
               .orderByAsc(LabTimeSlot::getDate)
               .orderByAsc(LabTimeSlot::getStartTime);
        Page<LabTimeSlot> slotPage = labTimeSlotMapper.selectPage(page, wrapper);

        Page<Map<String, Object>> result = new Page<>(pageNum, pageSize, slotPage.getTotal());
        List<Map<String, Object>> records = new ArrayList<>();
        for (LabTimeSlot slot : slotPage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("timeSlot", slot);
            LabCourse lab = labCourseMapper.selectById(slot.getLabId());
            item.put("labName", lab != null ? lab.getCourseName() : "未知实验室");
            item.put("remaining", slot.getCapacity() - slot.getBookedCount());
            records.add(item);
        }
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createReservation(Reservation reservation) {
        if (!UserContext.isStudent()) {
            throw new BusinessException(403, "只有学生可以预约实验");
        }
        Long studentId = UserContext.getUserId();
        Long timeSlotId = reservation.getTimeSlotId();

        LabTimeSlot slot = labTimeSlotMapper.selectById(timeSlotId);
        if (slot == null) {
            throw new BusinessException(404, "时间段不存在");
        }
        if (slot.getDate() == null || slot.getStartTime() == null || slot.getEndTime() == null) {
            throw new BusinessException(400, "时间段信息不完整");
        }

        LambdaQueryWrapper<Reservation> conflictWrapper = new LambdaQueryWrapper<>();
        conflictWrapper.eq(Reservation::getStudentId, studentId)
                       .eq(Reservation::getTimeSlotId, timeSlotId)
                       .in(Reservation::getStatus, "PENDING", "APPROVED");
        if (reservationMapper.selectCount(conflictWrapper) > 0) {
            throw new BusinessException(400, "您已预约过该时间段");
        }
        assertNoTimeConflict(studentId, slot);

        Long targetExperimentId = slot.getExperimentId() != null ? slot.getExperimentId() : reservation.getExperimentId();
        assertCourseNotArchived(targetExperimentId);
        admissionService.assertReservable(studentId, targetExperimentId);

        LambdaUpdateWrapper<LabTimeSlot> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LabTimeSlot::getId, timeSlotId)
                     .apply("booked_count < capacity")
                     .eq(LabTimeSlot::getStatus, "AVAILABLE")
                     .setSql("booked_count = booked_count + 1");
        int updated = labTimeSlotMapper.update(updateWrapper);
        if (updated == 0) {
            throw new BusinessException(400, "名额已满或时间段不可用");
        }

        reservation.setStudentId(studentId);
        reservation.setLabId(slot.getLabId());
        reservation.setExperimentId(targetExperimentId);
        reservation.setStatus("PENDING");
        reservation.setCreateTime(new java.util.Date());
        reservationMapper.insert(reservation);

        Map<String, Object> result = new HashMap<>();
        result.put("id", reservation.getId());
        result.put("status", "PENDING");
        return result;
    }

    private void assertNoTimeConflict(Long studentId, LabTimeSlot targetSlot) {
        List<Reservation> activeReservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStudentId, studentId)
                .in(Reservation::getStatus, "PENDING", "APPROVED"));
        for (Reservation active : activeReservations) {
            LabTimeSlot existingSlot = labTimeSlotMapper.selectById(active.getTimeSlotId());
            if (existingSlot == null || existingSlot.getDate() == null
                    || existingSlot.getStartTime() == null || existingSlot.getEndTime() == null) {
                continue;
            }
            if (sameDay(targetSlot.getDate(), existingSlot.getDate())
                    && overlaps(targetSlot.getStartTime(), targetSlot.getEndTime(),
                    existingSlot.getStartTime(), existingSlot.getEndTime())) {
                throw new BusinessException(400, "您在该时间段已有其他实验预约");
            }
        }
    }

    private boolean sameDay(Date left, Date right) {
        Calendar a = Calendar.getInstance();
        a.setTime(left);
        Calendar b = Calendar.getInstance();
        b.setTime(right);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private boolean overlaps(Date start, Date end, Date otherStart, Date otherEnd) {
        return start.before(otherEnd) && end.after(otherStart);
    }

    private void assertCourseNotArchived(Long experimentId) {
        if (experimentId == null) {
            return;
        }
        Experiment experiment = experimentMapper.selectById(experimentId);
        if (experiment == null) {
            return;
        }
        LabCourse course = labCourseMapper.selectById(experiment.getCourseId());
        if (course != null && course.getStatus() != null && course.getStatus() == 2) {
            throw new BusinessException(409, "课程已归档，不能新增预约");
        }
    }

    private void assertExperimentReservable(Long experimentId) {
        if (experimentId == null) {
            return;
        }
        Experiment experiment = experimentMapper.selectById(experimentId);
        if (experiment == null) {
            throw new BusinessException(404, "实验项目不存在");
        }
        if (experiment.getStatus() == null || experiment.getStatus() != 1) {
            throw new BusinessException(409, "实验未开放或处于维护状态，不能生成可预约时段");
        }
        if (!Integer.valueOf(1).equals(experiment.getReservationEnabled())) {
            throw new BusinessException(409, "实验未启用预约，不能生成可预约时段");
        }
    }

    private void assertExperimentWritable(Long experimentId) {
        if (!UserContext.isTeacher()) {
            if (UserContext.isAdmin() || UserContext.isLabAdmin()) {
                return;
            }
            throw new BusinessException(403, "无权维护实验预约");
        }
        Experiment experiment = experimentMapper.selectById(experimentId);
        if (experiment == null) {
            throw new BusinessException(404, "实验项目不存在");
        }
        LabCourse course = labCourseMapper.selectById(experiment.getCourseId());
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        if (!UserContext.getUserId().equals(course.getTeacherId())) {
            throw new BusinessException(403, "不能维护非本人负责课程的预约");
        }
    }

    private List<Long> teacherExperimentIds() {
        List<Long> courseIds = labCourseMapper.selectList(new LambdaQueryWrapper<LabCourse>()
                        .select(LabCourse::getId)
                        .eq(LabCourse::getTeacherId, UserContext.getUserId()))
                .stream()
                .map(LabCourse::getId)
                .toList();
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return experimentMapper.selectList(new LambdaQueryWrapper<Experiment>()
                        .select(Experiment::getId)
                        .in(Experiment::getCourseId, courseIds))
                .stream()
                .map(Experiment::getId)
                .toList();
    }

    @Override
    public Page<Reservation> getMyReservations(int pageNum, int pageSize, String status) {
        Page<Reservation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getStudentId, UserContext.getUserId())
               .eq(status != null && !status.isEmpty(), Reservation::getStatus, status)
               .orderByDesc(Reservation::getCreateTime);
        return reservationMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public void cancelReservation(Long id) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null || !"PENDING".equals(reservation.getStatus())) {
            throw new BusinessException(400, "预约状态不允许取消");
        }
        if (!UserContext.getUserId().equals(reservation.getStudentId())) {
            throw new BusinessException(403, "不能取消他人的预约");
        }
        reservation.setStatus("CANCELLED");
        reservationMapper.updateById(reservation);

        // 閲婃斁鍚嶉
        LambdaUpdateWrapper<LabTimeSlot> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LabTimeSlot::getId, reservation.getTimeSlotId())
                     .apply("booked_count > 0")
                     .setSql("booked_count = booked_count - 1");
        labTimeSlotMapper.update(updateWrapper);
    }

    @Override
    public Page<ReservationVO> getPendingReservations(int pageNum, int pageSize, Long labId) {
        Page<Reservation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getStatus, "PENDING")
               .eq(labId != null, Reservation::getLabId, labId)
               .orderByAsc(Reservation::getCreateTime);
        if (UserContext.isTeacher()) {
            List<Long> experimentIds = teacherExperimentIds();
            if (experimentIds.isEmpty()) {
                Page<ReservationVO> empty = new Page<>(pageNum, pageSize, 0);
                empty.setRecords(new ArrayList<>());
                return empty;
            }
            wrapper.in(Reservation::getExperimentId, experimentIds);
        }
        Page<Reservation> reservationPage = reservationMapper.selectPage(page, wrapper);
        Page<ReservationVO> result = new Page<>(pageNum, pageSize, reservationPage.getTotal());
        result.setRecords(reservationPage.getRecords().stream().map(this::toReservationVO).toList());
        return result;
    }

    private ReservationVO toReservationVO(Reservation reservation) {
        ReservationVO vo = new ReservationVO();
        vo.setId(reservation.getId());
        vo.setStudentId(reservation.getStudentId());
        vo.setTimeSlotId(reservation.getTimeSlotId());
        vo.setLabId(reservation.getLabId());
        vo.setExperimentId(reservation.getExperimentId());
        vo.setPurpose(reservation.getPurpose());
        vo.setStatus(reservation.getStatus());
        vo.setTeacherId(reservation.getTeacherId());
        vo.setReviewComment(reservation.getReviewComment());
        vo.setReviewTime(reservation.getReviewTime());
        vo.setCreateTime(reservation.getCreateTime());

        User student = userMapper.selectById(reservation.getStudentId());
        if (student != null) {
            String realName = student.getRealName() == null || student.getRealName().isBlank()
                    ? student.getUsername()
                    : student.getRealName();
            vo.setStudentName(realName);
        }

        LabTimeSlot slot = labTimeSlotMapper.selectById(reservation.getTimeSlotId());
        if (slot != null) {
            vo.setDate(slot.getDate());
            vo.setTimeRange(formatTime(slot.getStartTime()) + " - " + formatTime(slot.getEndTime()));
            if (vo.getExperimentId() == null) {
                vo.setExperimentId(slot.getExperimentId());
            }
        }
        LabCourse lab = labCourseMapper.selectById(reservation.getLabId());
        vo.setLabName(lab == null ? "实验室 " + reservation.getLabId() : lab.getCourseName());
        return vo;
    }

    private String formatTime(Date time) {
        return time == null ? "--:--" : new SimpleDateFormat("HH:mm").format(time);
    }

    @Override
    @Transactional
    public void reviewReservation(Long id, String status, String reviewComment) {
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new BusinessException(400, "审核状态只能为APPROVED或REJECTED");
        }
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null || !"PENDING".equals(reservation.getStatus())) {
            throw new BusinessException(400, "预约状态不允许审核");
        }
        assertExperimentWritable(reservation.getExperimentId());
        reservation.setStatus(status);
        reservation.setReviewComment(reviewComment);
        reservation.setTeacherId(UserContext.getUserId());
        reservation.setReviewTime(new java.util.Date());
        reservationMapper.updateById(reservation);

        // 濡傛灉鎷掔粷锛岄噴鏀惧悕棰?
        if ("REJECTED".equals(status)) {
            LambdaUpdateWrapper<LabTimeSlot> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(LabTimeSlot::getId, reservation.getTimeSlotId())
                         .apply("booked_count > 0")
                         .setSql("booked_count = booked_count - 1");
            labTimeSlotMapper.update(updateWrapper);
        }
        portalMessageService.send(reservation.getStudentId(),
                "预约审核" + ("APPROVED".equals(status) ? "通过" : "未通过"),
                reviewComment == null || reviewComment.isBlank() ? "请查看实验预约详情" : reviewComment,
                "RESERVATION_REVIEW",
                reservation.getId(),
                "/student/reserve");
    }
}
