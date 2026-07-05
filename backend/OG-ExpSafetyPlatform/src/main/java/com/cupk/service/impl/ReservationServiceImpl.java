package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.mapper.*;
import com.cupk.pojo.*;
import com.cupk.service.ReservationService;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private LabCourseMapper labCourseMapper;

    @Override
    public Page<LabTimeSlot> pageTimeSlots(int pageNum, int pageSize, Long labId, String date, String status) {
        Page<LabTimeSlot> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LabTimeSlot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(labId != null, LabTimeSlot::getLabId, labId)
               .eq(date != null && !date.isEmpty(), LabTimeSlot::getDate, date)
               .eq(status != null && !status.isEmpty(), LabTimeSlot::getStatus, status)
               .orderByDesc(LabTimeSlot::getDate)
               .orderByAsc(LabTimeSlot::getStartTime);
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
        slot.setId(id);
        labTimeSlotMapper.updateById(slot);
    }

    @Override
    public void deleteTimeSlot(Long id) {
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
        Long studentId = UserContext.getUserId();
        Long timeSlotId = reservation.getTimeSlotId();

        LabTimeSlot slot = labTimeSlotMapper.selectById(timeSlotId);
        if (slot == null) {
            throw new BusinessException(404, "时间段不存在");
        }

        LambdaQueryWrapper<Reservation> conflictWrapper = new LambdaQueryWrapper<>();
        conflictWrapper.eq(Reservation::getStudentId, studentId)
                       .eq(Reservation::getTimeSlotId, timeSlotId)
                       .in(Reservation::getStatus, "PENDING", "APPROVED");
        if (reservationMapper.selectCount(conflictWrapper) > 0) {
            throw new BusinessException(400, "您已预约过该时间段");
        }

        Long targetExperimentId = slot.getExperimentId() != null ? slot.getExperimentId() : reservation.getExperimentId();
        LambdaQueryWrapper<ExamRecord> examWrapper = new LambdaQueryWrapper<>();
        examWrapper.eq(ExamRecord::getStudentId, studentId)
                   .eq(ExamRecord::getPassed, 1)
                   .in(ExamRecord::getStatus, "SUBMITTED", "REVIEWED")
                   .eq(targetExperimentId != null, ExamRecord::getExperimentId, targetExperimentId);
        if (examRecordMapper.selectCount(examWrapper) == 0) {
            throw new BusinessException(403, "请先通过对应安全考试后再预约实验");
        }

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
    public Page<Reservation> getPendingReservations(int pageNum, int pageSize, Long labId) {
        Page<Reservation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getStatus, "PENDING")
               .eq(labId != null, Reservation::getLabId, labId)
               .orderByAsc(Reservation::getCreateTime);
        return reservationMapper.selectPage(page, wrapper);
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
    }
}
