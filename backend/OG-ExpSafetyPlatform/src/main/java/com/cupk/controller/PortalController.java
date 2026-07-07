package com.cupk.controller;

import com.cupk.common.Result;
import com.cupk.common.RequirePermission;
import com.cupk.dto.RecentVisitDTO;
import com.cupk.dto.ShortcutUpdateDTO;
import com.cupk.service.PortalService;
import com.cupk.vo.CalendarEventVO;
import com.cupk.vo.PortalHomeVO;
import com.cupk.vo.PortalItemVO;
import com.cupk.vo.SearchResultVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portal")
public class PortalController {
    private final PortalService portalService;

    public PortalController(PortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/home")
    @RequirePermission("portal:view")
    public Result<PortalHomeVO> home() {
        return Result.success(portalService.home());
    }

    @GetMapping("/notices")
    @RequirePermission("portal:view")
    public Result<List<PortalItemVO>> notices(@RequestParam(required = false) Integer limit) {
        return Result.success(portalService.notices(limit));
    }

    @GetMapping("/messages")
    @RequirePermission("portal:message")
    public Result<List<PortalItemVO>> messages(@RequestParam(required = false) Integer limit) {
        return Result.success(portalService.messages(limit));
    }

    @GetMapping("/messages/unread-count")
    @RequirePermission("portal:message")
    public Result<Integer> unreadMessages() {
        return Result.success(portalService.unreadMessages());
    }

    @PutMapping("/messages/{id}/read")
    @RequirePermission("portal:message")
    public Result<Void> markMessageRead(@PathVariable Long id) {
        portalService.markMessageRead(id);
        return Result.success();
    }

    @GetMapping("/calendar")
    @RequirePermission("portal:view")
    public Result<List<CalendarEventVO>> calendar(@RequestParam(required = false) Integer limit) {
        return Result.success(portalService.calendar(limit));
    }

    @GetMapping("/search")
    @RequirePermission("portal:search")
    public Result<List<SearchResultVO>> search(@RequestParam String keyword,
                                               @RequestParam(required = false) Integer limit) {
        return Result.success(portalService.search(keyword, limit));
    }

    @GetMapping("/recent-visits")
    @RequirePermission("portal:view")
    public Result<List<PortalItemVO>> recentVisits(@RequestParam(required = false) Integer limit) {
        return Result.success(portalService.recentVisits(limit));
    }

    @PostMapping("/recent-visits")
    @RequirePermission("portal:view")
    public Result<Void> recordVisit(@Valid @RequestBody RecentVisitDTO dto) {
        portalService.recordVisit(dto);
        return Result.success();
    }

    @GetMapping("/shortcuts")
    @RequirePermission("portal:view")
    public Result<List<PortalItemVO>> shortcuts(@RequestParam(required = false) Integer limit) {
        return Result.success(portalService.shortcuts(limit));
    }

    @PutMapping("/shortcuts")
    @RequirePermission("portal:view")
    public Result<Void> saveShortcuts(@Valid @RequestBody List<ShortcutUpdateDTO> dtos) {
        portalService.saveShortcuts(dtos);
        return Result.success();
    }
}
