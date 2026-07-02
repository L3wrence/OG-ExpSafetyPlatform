package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.mapper.ExamPaperMapper;
import com.cupk.mapper.ExamPaperQuestionMapper;
import com.cupk.mapper.QuestionMapper;
import com.cupk.pojo.ExamPaper;
import com.cupk.pojo.ExamPaperQuestion;
import com.cupk.pojo.Question;
import com.cupk.service.ExamPaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 试卷管理服务实现
 */
@Service
public class ExamPaperServiceImpl implements ExamPaperService {

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @Autowired
    private ExamPaperQuestionMapper examPaperQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Override
    public Page<ExamPaper> pagePapers(int pageNum, int pageSize, String keyword,
                                       Long courseId, String status) {
        Page<ExamPaper> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(courseId != null, ExamPaper::getCourseId, courseId)
               .eq(StringUtils.hasText(status), ExamPaper::getStatus, status)
               .like(StringUtils.hasText(keyword), ExamPaper::getTitle, keyword)
               .orderByDesc(ExamPaper::getCreateTime);

        return examPaperMapper.selectPage(page, wrapper);
    }

    @Override
    public Map<String, Object> getPaperDetail(Long id) {
        ExamPaper paper = examPaperMapper.selectById(id);
        if (paper == null) {
            return null;
        }
        // 查询关联的题目列表（按排序号排序）
        LambdaQueryWrapper<ExamPaperQuestion> eqWrapper = new LambdaQueryWrapper<>();
        eqWrapper.eq(ExamPaperQuestion::getPaperId, id)
                 .orderByAsc(ExamPaperQuestion::getOrderNum);
        List<ExamPaperQuestion> eqList = examPaperQuestionMapper.selectList(eqWrapper);

        // 组装题目详情
        List<Map<String, Object>> questions = new ArrayList<>();
        for (ExamPaperQuestion eq : eqList) {
            Question q = questionMapper.selectById(eq.getQuestionId());
            if (q != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("question", q);
                item.put("score", eq.getScore());
                item.put("orderNum", eq.getOrderNum());
                questions.add(item);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("paper", paper);
        result.put("questions", questions);
        return result;
    }

    @Override
    public Long createPaper(ExamPaper paper) {
        paper.setStatus("DRAFT");
        examPaperMapper.insert(paper);
        return paper.getId();
    }

    @Override
    public void updatePaper(Long id, ExamPaper paper) {
        paper.setId(id);
        examPaperMapper.updateById(paper);
    }

    @Override
    public void deletePaper(Long id) {
        examPaperMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, String status) {
        ExamPaper paper = new ExamPaper();
        paper.setId(id);
        paper.setStatus(status);
        examPaperMapper.updateById(paper);
    }

    @Override
    @Transactional
    public void addQuestions(Long paperId, List<Long> questionIds, List<Integer> scores) {
        // 先获取当前最大排序号
        LambdaQueryWrapper<ExamPaperQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaperQuestion::getPaperId, paperId)
               .orderByDesc(ExamPaperQuestion::getOrderNum)
               .last("LIMIT 1");
        ExamPaperQuestion last = examPaperQuestionMapper.selectOne(wrapper);
        int nextOrder = (last != null) ? last.getOrderNum() + 1 : 1;

        for (int i = 0; i < questionIds.size(); i++) {
            ExamPaperQuestion eq = new ExamPaperQuestion();
            eq.setPaperId(paperId);
            eq.setQuestionId(questionIds.get(i));
            eq.setScore(scores != null && i < scores.size() ? scores.get(i) : 0);
            eq.setOrderNum(nextOrder + i);
            examPaperQuestionMapper.insert(eq);
        }

        // 重新计算试卷总分
        recalcTotalScore(paperId);
    }

    @Override
    @Transactional
    public void removeQuestion(Long paperId, Long questionId) {
        LambdaQueryWrapper<ExamPaperQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaperQuestion::getPaperId, paperId)
               .eq(ExamPaperQuestion::getQuestionId, questionId);
        examPaperQuestionMapper.delete(wrapper);
        recalcTotalScore(paperId);
    }

    @Override
    @Transactional
    public void updateQuestionOrder(Long paperId, List<Map<String, Integer>> orders) {
        for (Map<String, Integer> order : orders) {
            Long questionId = order.get("questionId").longValue();
            Integer orderNum = order.get("orderNum");

            LambdaQueryWrapper<ExamPaperQuestion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ExamPaperQuestion::getPaperId, paperId)
                   .eq(ExamPaperQuestion::getQuestionId, questionId);

            ExamPaperQuestion eq = new ExamPaperQuestion();
            eq.setOrderNum(orderNum);
            examPaperQuestionMapper.update(eq, wrapper);
        }
    }

    /** 重新计算试卷总分 */
    private void recalcTotalScore(Long paperId) {
        LambdaQueryWrapper<ExamPaperQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaperQuestion::getPaperId, paperId);
        List<ExamPaperQuestion> eqList = examPaperQuestionMapper.selectList(wrapper);

        int total = eqList.stream().mapToInt(ExamPaperQuestion::getScore).sum();
        ExamPaper paper = new ExamPaper();
        paper.setId(paperId);
        paper.setTotalScore(total);
        examPaperMapper.updateById(paper);
    }
}
