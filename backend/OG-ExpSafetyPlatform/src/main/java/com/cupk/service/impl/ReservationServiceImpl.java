package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.mapper.*;
import com.cupk.pojo.*;
import com.cupk.service.ReservationService;
import com.cupk.exception.BusinessException;
import com.cupk.common.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 实验预约服务实现（核心：含资格校验+并发控制）
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
               .eq(status != null && !status.isEmpty(), LabTimeSlot::getStatus, status)
               .orderByDesc(LabTimeSlot::getDate)
               .orderByAsc(LabTimeSlot::getStartTime);
        return labTimeSlotMapper.selectPage(page, wrapper);
    }

    @Override
    public Map<String, Integer> batchCreateTimeSlots(List<LabTimeSlot> slots) {
        int successCount = 0;
        int failCount = 0;
        for (LabTimeSlot slot : slots) {
            try {
                slot.setBookedCount(0);
                slot.setStatus("AVAILABLE");
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

        // 1. 并发控制：使用乐观锁更新已预约人数
        LambdaUpdateWrapper<LabTimeSlot> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LabTimeSlot::getId, timeSlotId)
                     .apply("booked_count < capacity")
                     .eq(LabTimeSlot::getStatus, "AVAILABLE")
                     .setSql("booked_count = booked_count + 1");
        int updated = labTimeSlotMapper.update(updateWrapper);
        if (updated == 0) {
            throw new BusinessException(400, "名额已满或时间段不可用");
        }

        // 2. 冲突检查：不能重复预约同一时间段
        LambdaQueryWrapper<Reservation> conflictWrapper = new LambdaQueryWrapper<>();
        conflictWrapper.eq(Reservation::getStudentId, studentId)
                       .eq(Reservation::getTimeSlotId, timeSlotId)
                       .in(Reservation::getStatus, "PENDING", "APPROVED");
        if (reservationMapper.selectCount(conflictWrapper) > 0) {
            throw new BusinessException(400, "您已预约过该时间段");
        }

        // 3. ★ 资格校验：必须通过安全考试才能预约实验
        LambdaQueryWrapper<ExamRecord> examWrapper = new LambdaQueryWrapper<>();
        examWrapper.eq(ExamRecord::getStudentId, studentId)
                   .eq(ExamRecord::getPassed, 1)
                   .eq(ExamRecord::getStatus, "SUBMITTED");
        if (examRecordMapper.selectCount(examWrapper) == 0) {
            // 回滚容量
            LambdaUpdateWrapper<LabTimeSlot> rollbackWrapper = new LambdaUpdateWrapper<>();
            rollbackWrapper.eq(LabTimeSlot::getId, timeSlotId)
                          .setSql("booked_count = booked_count - 1");
            labTimeSlotMapper.update(rollbackWrapper);
            throw new BusinessException(403, "请先通过安全考试后再预约实验");
        }

        // 4. 创建预约记录
        reservation.setStudentId(studentId);
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
        reservation.setStatus("CANCELLED");
        reservationMapper.updateById(reservation);

        // 释放名额
        LambdaUpdateWrapper<LabTimeSlot> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LabTimeSlot::getId, reservation.getTimeSlotId())
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
    public void reviewReservation(Long id, String status, String reviewComment) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null || !"PENDING".equals(reservation.getStatus())) {
            throw new BusinessException(400, "预约状态不允许审核");
        }
        reservation.setStatus(status);
        reservation.setReviewComment(reviewComment);
        reservation.setTeacherId(UserContext.getUserId());
        reservation.setReviewTime(new java.util.Date());
        reservationMapper.updateById(reservation);

        // 如果拒绝，释放名额
        if ("REJECTED".equals(status)) {
            LambdaUpdateWrapper<LabTimeSlot> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(LabTimeSlot::getId, reservation.getTimeSlotId())
                         .setSql("booked_count = booked_count - 1");
            labTimeSlotMapper.update(updateWrapper);
        }
    }
}
