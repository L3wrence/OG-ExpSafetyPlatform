package com.cupk.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.pojo.LabTimeSlot;
import com.cupk.pojo.Reservation;
import com.cupk.vo.ReservationVO;

import java.util.List;
import java.util.Map;

/**
 * 实验预约服务接口（核心：含资格校验+并发控制）
 */
public interface ReservationService {

    // ===== 时间段管理 =====

    /** 分页查询时间段 */
    Page<LabTimeSlot> pageTimeSlots(int pageNum, int pageSize, Long labId, String date, String status);

    /** 批量创建时间段 */
    Map<String, Integer> batchCreateTimeSlots(List<LabTimeSlot> slots);

    /** 修改时间段 */
    void updateTimeSlot(Long id, LabTimeSlot slot);

    /** 删除时间段 */
    void deleteTimeSlot(Long id);

    // ===== 学生预约 =====

    /** 可预约时间段（含剩余容量） */
    Page<Map<String, Object>> getAvailableSlots(Long labId, Long experimentId, String date, int pageNum, int pageSize);

    /** 提交预约申请（含资格校验+并发控制） */
    Map<String, Object> createReservation(Reservation reservation);

    /** 我的预约列表 */
    Page<Reservation> getMyReservations(int pageNum, int pageSize, String status);

    /** 取消预约 */
    void cancelReservation(Long id);

    // ===== 教师审核 =====

    /** 待审核预约列表 */
    Page<ReservationVO> getPendingReservations(int pageNum, int pageSize, Long labId);

    /** 审核预约 */
    void reviewReservation(Long id, String status, String reviewComment);
}
