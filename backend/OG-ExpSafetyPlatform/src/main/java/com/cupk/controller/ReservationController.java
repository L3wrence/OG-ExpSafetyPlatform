package com.cupk.controller;

import com.cupk.pojo.LabTimeSlot;
import com.cupk.pojo.Reservation;
import com.cupk.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 实验预约接口
 * 路径：/api/reservations
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    // ===== 时间段管理（教师端） =====

    /** 分页查询时间段 */
    @GetMapping("/time-slots")
    public Result<?> timeSlots(@RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) Long labId,
                                @RequestParam(required = false) String date,
                                @RequestParam(required = false) String status) {
        return Result.success(reservationService.pageTimeSlots(pageNum, pageSize, labId, date, status));
    }

    /** 批量创建时间段 */
    @PostMapping("/time-slots")
    public Result<Map<String, Integer>> createTimeSlots(@RequestBody List<LabTimeSlot> slots) {
        return Result.success(reservationService.batchCreateTimeSlots(slots));
    }

    /** 修改时间段 */
    @PutMapping("/time-slots/{id}")
    public Result<?> updateTimeSlot(@PathVariable Long id, @RequestBody LabTimeSlot slot) {
        reservationService.updateTimeSlot(id, slot);
        return Result.success();
    }

    /** 删除时间段 */
    @DeleteMapping("/time-slots/{id}")
    public Result<?> deleteTimeSlot(@PathVariable Long id) {
        reservationService.deleteTimeSlot(id);
        return Result.success();
    }

    // ===== 学生预约 =====

    /** 可预约时间段 */
    @GetMapping("/available-slots")
    public Result<?> availableSlots(@RequestParam(required = false) Long labId,
                                     @RequestParam(required = false) String date,
                                     @RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(reservationService.getAvailableSlots(labId, date, pageNum, pageSize));
    }

    /** 提交预约申请 */
    @PostMapping
    public Result<?> create(@RequestBody Reservation reservation) {
        return Result.success(reservationService.createReservation(reservation));
    }

    /** 我的预约列表 */
    @GetMapping("/my")
    public Result<?> myReservations(@RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize,
                                     @RequestParam(required = false) String status) {
        return Result.success(reservationService.getMyReservations(pageNum, pageSize, status));
    }

    /** 取消预约 */
    @PutMapping("/{id}/cancel")
    public Result<?> cancel(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return Result.success();
    }

    // ===== 教师审核 =====

    /** 待审核预约列表 */
    @GetMapping("/pending")
    public Result<?> pending(@RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize,
                              @RequestParam(required = false) Long labId) {
        return Result.success(reservationService.getPendingReservations(pageNum, pageSize, labId));
    }

    /** 审核预约 */
    @PutMapping("/{id}/review")
    public Result<?> review(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reservationService.reviewReservation(id, body.get("status"), body.get("reviewComment"));
        return Result.success();
    }
}
