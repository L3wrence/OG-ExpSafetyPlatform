package com.cupk.controller;

import com.cupk.pojo.ExamPaper;
import com.cupk.common.RequirePermission;
import com.cupk.common.Result;
import com.cupk.interceptor.UserContext;
import com.cupk.dto.exam.PaperCreateDTO;
import com.cupk.service.ExamPaperService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 璇曞嵎绠＄悊鎺ュ彛锛堟暀甯堢锛?
 * 璺緞锛?api/exams/papers
 */
@RestController
@RequestMapping("/api/exams/papers")
public class ExamPaperController {

    @Autowired
    private ExamPaperService examPaperService;

    /** 鍒嗛〉鏌ヨ璇曞嵎 */
    @RequirePermission("exam-paper:view")
    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long courseId,
                          @RequestParam(required = false) String status) {
        return Result.success(examPaperService.pagePapers(pageNum, pageSize, keyword, courseId, status));
    }

    /** 璇曞嵎璇︽儏锛堝惈棰樼洰鍒楄〃锛?*/
    @RequirePermission("exam-paper:view")
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(examPaperService.getPaperDetail(id));
    }

    /** 鍒涘缓璇曞嵎 */
    @RequirePermission("exam:create")
    @PostMapping
    public Result<Map<String, Long>> create(@Valid @RequestBody PaperCreateDTO dto) {
        ExamPaper paper = new ExamPaper();
        BeanUtils.copyProperties(dto, paper);
        paper.setTeacherId(UserContext.getUserId());
        return Result.success(Map.of("id", examPaperService.createPaper(paper)));
    }

    /** 缂栬緫璇曞嵎淇℃伅 */
    @RequirePermission("exam:update")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody PaperCreateDTO dto) {
        ExamPaper paper = new ExamPaper();
        BeanUtils.copyProperties(dto, paper);
        examPaperService.updatePaper(id, paper);
        return Result.success();
    }

    /** 閫昏緫鍒犻櫎璇曞嵎 */
    @RequirePermission("exam:delete")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        examPaperService.deletePaper(id);
        return Result.success();
    }

    /** 鍙戝竷/鍏抽棴璇曞嵎 */
    @RequirePermission("exam:update")
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        examPaperService.updateStatus(id, body.get("status"));
        return Result.success();
    }

    /** 鍚戣瘯鍗锋坊鍔犻鐩?*/
    @RequirePermission("exam:update")
    @PostMapping("/{id}/questions")
    public Result<?> addQuestions(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> questionIds = ((List<?>) body.get("questionIds")).stream()
                .map(value -> Long.valueOf(value.toString())).toList();
        @SuppressWarnings("unchecked")
        List<Integer> scores = body.get("scores") == null ? null : ((List<?>) body.get("scores")).stream()
                .map(value -> Integer.valueOf(value.toString())).toList();
        examPaperService.addQuestions(id, questionIds, scores);
        return Result.success();
    }

    /** 浠庤瘯鍗风Щ闄ら鐩?*/
    @RequirePermission("exam:update")
    @DeleteMapping("/{id}/questions/{qid}")
    public Result<?> removeQuestion(@PathVariable Long id, @PathVariable Long qid) {
        examPaperService.removeQuestion(id, qid);
        return Result.success();
    }

    @RequirePermission("exam:update")
    @PutMapping("/{id}/questions/{qid}/score")
    public Result<?> updateQuestionScore(@PathVariable Long id,
                                         @PathVariable Long qid,
                                         @RequestBody Map<String, Object> body) {
        examPaperService.updateQuestionScore(id, qid, Integer.valueOf(body.get("score").toString()));
        return Result.success();
    }

    @RequirePermission("exam:update")
    @PostMapping("/{id}/subjective-questions")
    public Result<?> addSubjectiveQuestion(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        examPaperService.addSubjectiveQuestion(id, body);
        return Result.success();
    }

    /** 璋冩暣棰樼洰鎺掑簭 */
    @RequirePermission("exam:update")
    @PutMapping("/{id}/questions/order")
    public Result<?> updateOrder(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Integer>> orders = (List<Map<String, Integer>>) body.get("orders");
        examPaperService.updateQuestionOrder(id, orders);
        return Result.success();
    }

    /** 智能组卷：按题型、知识点、难度、实验范围抽题后写入试卷 */
    @RequirePermission("exam:update")
    @PostMapping("/{id}/smart-assemble")
    public Result<?> smartAssemble(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.success(examPaperService.smartAssemble(id, body));
    }
}
