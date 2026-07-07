package com.cupk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cupk.exception.BusinessException;
import com.cupk.interceptor.UserContext;
import com.cupk.mapper.ExperimentMapper;
import com.cupk.mapper.LabCourseMapper;
import com.cupk.mapper.QuestionMapper;
import com.cupk.pojo.Experiment;
import com.cupk.pojo.LabCourse;
import com.cupk.pojo.Question;
import com.cupk.service.QuestionService;
import com.cupk.util.AccessUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 题目管理服务实现
 */
@Service
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private LabCourseMapper labCourseMapper;

    @Autowired
    private ExperimentMapper experimentMapper;

    @Override
    public Page<Question> pageQuestions(int pageNum, int pageSize, String type,
                                         String difficulty, String keyword, Long courseId,
                                         Long experimentId, Long knowledgeId,
                                         String knowledgePoint, String riskType) {
        Page<Question> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(StringUtils.hasText(type), Question::getType, type)
               .eq(StringUtils.hasText(difficulty), Question::getDifficulty, difficulty)
               .eq(courseId != null, Question::getCourseId, courseId)
               .eq(experimentId != null, Question::getExperimentId, experimentId)
               .eq(knowledgeId != null, Question::getKnowledgeId, knowledgeId)
               .eq(StringUtils.hasText(knowledgePoint), Question::getKnowledgePoint, knowledgePoint)
               .eq(StringUtils.hasText(riskType), Question::getRiskType, riskType)
               .like(StringUtils.hasText(keyword), Question::getContent, keyword)
               .orderByDesc(Question::getCreateTime);
        if (UserContext.isTeacher()) {
            List<Long> courseIds = teacherCourseIds();
            if (courseIds.isEmpty()) {
                page.setRecords(List.of());
                page.setTotal(0);
                return page;
            }
            wrapper.in(Question::getCourseId, courseIds);
        }

        return questionMapper.selectPage(page, wrapper);
    }

    @Override
    public Question getQuestionById(Long id) {
        Question question = questionMapper.selectById(id);
        if (question != null) {
            assertQuestionReadable(question);
        }
        return question;
    }

    @Override
    public Long addQuestion(Question question) {
        normalizeQuestionScope(question);
        assertCourseWritable(question.getCourseId());
        questionMapper.insert(question);
        return question.getId();
    }

    @Override
    public Map<String, Integer> batchAddQuestions(List<Question> questions) {
        int successCount = 0;
        int failCount = 0;
        for (Question q : questions) {
            try {
                normalizeQuestionScope(q);
                assertCourseWritable(q.getCourseId());
                questionMapper.insert(q);
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
    public void updateQuestion(Long id, Question question) {
        Question current = requireQuestion(id);
        assertCourseWritable(current.getCourseId());
        normalizeQuestionScope(question);
        assertCourseWritable(question.getCourseId());
        question.setId(id);
        question.setCreateBy(current.getCreateBy());
        questionMapper.updateById(question);
    }

    @Override
    public void deleteQuestion(Long id) {
        Question current = requireQuestion(id);
        assertCourseWritable(current.getCourseId());
        questionMapper.deleteById(id);  // MyBatis-Plus @TableLogic 自动实现逻辑删除
    }

    private void normalizeQuestionScope(Question question) {
        if (question.getExperimentId() != null) {
            Experiment experiment = experimentMapper.selectById(question.getExperimentId());
            if (experiment == null) {
                throw new BusinessException(404, "实验项目不存在");
            }
            if (question.getCourseId() == null) {
                question.setCourseId(experiment.getCourseId());
            } else if (!question.getCourseId().equals(experiment.getCourseId())) {
                throw new BusinessException(400, "题目课程与实验所属课程不一致");
            }
        }
        if (question.getCourseId() == null) {
            throw new BusinessException(400, "题目必须关联课程");
        }
    }

    private void assertQuestionReadable(Question question) {
        if (UserContext.isAdmin()) {
            return;
        }
        if (UserContext.isTeacher()) {
            assertCourseWritable(question.getCourseId());
            return;
        }
        throw new BusinessException(403, "无权查看该题目");
    }

    private void assertCourseWritable(Long courseId) {
        LabCourse course = labCourseMapper.selectById(courseId);
        AccessUtil.assertCourseWritable(course);
    }

    private Question requireQuestion(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(404, "题目不存在");
        }
        return question;
    }

    private List<Long> teacherCourseIds() {
        return labCourseMapper.selectList(new LambdaQueryWrapper<LabCourse>()
                        .select(LabCourse::getId)
                        .eq(LabCourse::getTeacherId, UserContext.getUserId()))
                .stream().map(LabCourse::getId).toList();
    }
}
