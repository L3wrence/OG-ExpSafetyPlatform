package com.cupk.controller;

import com.cupk.pojo.LabTimeSlot;
import com.cupk.pojo.Reservation;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.interceptor.UserContext;
import com.cupk.dto.reservation.ReservationCreateDTO;
import com.cupk.dto.reservation.ReviewDTO;
import com.cupk.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 瀹為獙棰勭害鎺ュ彛
 * 璺緞锛?api/reservations
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    // ===== 鏃堕棿娈电鐞嗭紙鏁欏笀绔級 =====

    /** 鍒嗛〉鏌ヨ鏃堕棿娈?*/
    @GetMapping("/time-slots")
    public Result<?> timeSlots(@RequestParam(defaultValue = "1") int pageNum,
                                @RequestParam(defaultValue = "10") int pageSize,
                                @RequestParam(required = false) Long labId,
                                @RequestParam(required = false) String date,
                                @RequestParam(required = false) String status) {
        return Result.success(reservationService.pageTimeSlots(pageNum, pageSize, labId, date, status));
    }

    /** 鎵归噺鍒涘缓鏃堕棿娈?*/
    @RequirePermission("reservation:manage")
    @PostMapping("/time-slots")
    public Result<Map<String, Integer>> createTimeSlots(@RequestBody List<LabTimeSlot> slots) {
        return Result.success(reservationService.batchCreateTimeSlots(slots));
    }

    /** 淇敼鏃堕棿娈?*/
    @RequirePermission("reservation:manage")
    @PutMapping("/time-slots/{id}")
    public Result<?> updateTimeSlot(@PathVariable Long id, @RequestBody LabTimeSlot slot) {
        reservationService.updateTimeSlot(id, slot);
        return Result.success();
    }

    /** 鍒犻櫎鏃堕棿娈?*/
    @RequirePermission("reservation:manage")
    @DeleteMapping("/time-slots/{id}")
    public Result<?> deleteTimeSlot(@PathVariable Long id) {
        reservationService.deleteTimeSlot(id);
        return Result.success();
    }

    // ===== 瀛︾敓棰勭害 =====

    /** 鍙绾︽椂闂存 */
    @GetMapping("/available-slots")
    public Result<?> availableSlots(@RequestParam(required = false) Long labId,
                                     @RequestParam(required = false) String date,
                                     @RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(reservationService.getAvailableSlots(labId, date, pageNum, pageSize));
    }

    /** 鎻愪氦棰勭害鐢宠 */
    @PostMapping
    public Result<?> create(@Valid @RequestBody ReservationCreateDTO dto) {
        Reservation reservation = new Reservation();
        BeanUtils.copyProperties(dto, reservation);
        reservation.setStudentId(UserContext.getUserId());
        return Result.success(reservationService.createReservation(reservation));
    }

    /** 鎴戠殑棰勭害鍒楄〃 */
    @GetMapping("/my")
    public Result<?> myReservations(@RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize,
                                     @RequestParam(required = false) String status) {
        return Result.success(reservationService.getMyReservations(pageNum, pageSize, status));
    }

    /** 鍙栨秷棰勭害 */
    @PutMapping("/{id}/cancel")
    public Result<?> cancel(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return Result.success();
    }

    // ===== 鏁欏笀瀹℃牳 =====

    /** 寰呭鏍搁绾﹀垪琛?*/
    @RequirePermission("reservation:review")
    @GetMapping("/pending")
    public Result<?> pending(@RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize,
                              @RequestParam(required = false) Long labId) {
        return Result.success(reservationService.getPendingReservations(pageNum, pageSize, labId));
    }

    /** 瀹℃牳棰勭害 */
    @RequirePermission("reservation:review")
    @PutMapping("/{id}/review")
    public Result<?> review(@PathVariable Long id, @Valid @RequestBody ReviewDTO dto) {
        reservationService.reviewReservation(id, dto.getStatus(), dto.getReviewComment());
        return Result.success();
    }
}
