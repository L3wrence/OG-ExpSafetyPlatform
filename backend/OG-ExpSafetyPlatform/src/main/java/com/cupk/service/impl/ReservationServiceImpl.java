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
            item.put("labName", lab != null ? lab.getCourseName() : "?????");
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

        // 1. 骞跺彂鎺у埗锛氫娇鐢ㄤ箰瑙傞攣鏇存柊宸查绾︿汉鏁?
        LambdaUpdateWrapper<LabTimeSlot> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LabTimeSlot::getId, timeSlotId)
                     .apply("booked_count < capacity")
                     .eq(LabTimeSlot::getStatus, "AVAILABLE")
                     .setSql("booked_count = booked_count + 1");
        int updated = labTimeSlotMapper.update(updateWrapper);
        if (updated == 0) {
            throw new BusinessException(400, "???????????");
        }

        // 2. 鍐茬獊妫€鏌ワ細涓嶈兘閲嶅棰勭害鍚屼竴鏃堕棿娈?
        LambdaQueryWrapper<Reservation> conflictWrapper = new LambdaQueryWrapper<>();
        conflictWrapper.eq(Reservation::getStudentId, studentId)
                       .eq(Reservation::getTimeSlotId, timeSlotId)
                       .in(Reservation::getStatus, "PENDING", "APPROVED");
        if (reservationMapper.selectCount(conflictWrapper) > 0) {
            throw new BusinessException(400, "?????????");
        }

        // 3. 鈽?璧勬牸鏍￠獙锛氬繀椤婚€氳繃瀹夊叏鑰冭瘯鎵嶈兘棰勭害瀹為獙
        LambdaQueryWrapper<ExamRecord> examWrapper = new LambdaQueryWrapper<>();
        examWrapper.eq(ExamRecord::getStudentId, studentId)
                   .eq(ExamRecord::getPassed, 1)
                   .eq(ExamRecord::getStatus, "SUBMITTED");
        if (examRecordMapper.selectCount(examWrapper) == 0) {
            // 鍥炴粴瀹归噺
            LambdaUpdateWrapper<LabTimeSlot> rollbackWrapper = new LambdaUpdateWrapper<>();
            rollbackWrapper.eq(LabTimeSlot::getId, timeSlotId)
                          .setSql("booked_count = booked_count - 1");
            labTimeSlotMapper.update(rollbackWrapper);
            throw new BusinessException(403, "璇峰厛閫氳繃瀹夊叏鑰冭瘯鍚庡啀棰勭害瀹為獙");
        }

        // 4. 鍒涘缓棰勭害璁板綍
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
            throw new BusinessException(400, "棰勭害鐘舵€佷笉鍏佽鍙栨秷");
        }
        reservation.setStatus("CANCELLED");
        reservationMapper.updateById(reservation);

        // 閲婃斁鍚嶉
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
            throw new BusinessException(400, "棰勭害鐘舵€佷笉鍏佽瀹℃牳");
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
                         .setSql("booked_count = booked_count - 1");
            labTimeSlotMapper.update(updateWrapper);
        }
    }
}
