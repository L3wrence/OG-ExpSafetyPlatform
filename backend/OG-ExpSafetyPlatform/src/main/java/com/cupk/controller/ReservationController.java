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
    public Map<String, Object> timeSlots(@RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "10") int pageSize,
                                          @RequestParam(required = false) Long labId,
                                          @RequestParam(required = false) String date,
                                          @RequestParam(required = false) String status) {
        // TODO
        return null;
    }

    /** 批量创建时间段 */
    @PostMapping("/time-slots")
    public Map<String, Integer> createTimeSlots(@RequestBody List<LabTimeSlot> slots) {
        // TODO
        return null;
    }

    /** 修改时间段 */
    @PutMapping("/time-slots/{id}")
    public void updateTimeSlot(@PathVariable Long id, @RequestBody LabTimeSlot slot) {
        // TODO
    }

    /** 删除时间段 */
    @DeleteMapping("/time-slots/{id}")
    public void deleteTimeSlot(@PathVariable Long id) {
        // TODO
    }

    // ===== 学生预约 =====

    /** 可预约时间段 */
    @GetMapping("/available-slots")
    public Map<String, Object> availableSlots(@RequestParam(required = false) Long labId,
                                               @RequestParam(required = false) String date,
                                               @RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize) {
        // TODO
        return null;
    }

    /** 提交预约申请 */
    @PostMapping
    public Map<String, Object> create(@RequestBody Reservation reservation) {
        // TODO
        return null;
    }

    /** 我的预约列表 */
    @GetMapping("/my")
    public Map<String, Object> myReservations(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               @RequestParam(required = false) String status) {
        // TODO
        return null;
    }

    /** 取消预约 */
    @PutMapping("/{id}/cancel")
    public void cancel(@PathVariable Long id) {
        // TODO
    }

    // ===== 教师审核 =====

    /** 待审核预约列表 */
    @GetMapping("/pending")
    public Map<String, Object> pending(@RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "10") int pageSize,
                                        @RequestParam(required = false) Long labId) {
        // TODO
        return null;
    }

    /** 审核预约 */
    @PutMapping("/{id}/review")
    public void review(@PathVariable Long id, @RequestBody Map<String, String> body) {
        // TODO
    }
}
