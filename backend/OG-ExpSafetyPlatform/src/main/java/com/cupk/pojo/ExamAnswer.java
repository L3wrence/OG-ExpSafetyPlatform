package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 答题明细表 t_exam_answer
 * 记录每道题的作答情况，与考试记录分开存储，便于逐题统计分析
 */
@Data
@TableName("t_exam_answer")
public class ExamAnswer {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 考试记录ID（引用 t_exam_record.id） */
    private Long recordId;

    /** 题目ID（引用 t_question.id） */
    private Long questionId;

    /** 学生提交的答案 */
    private String studentAnswer;

    /** 是否正确。客观题自动判定；简答题默认NULL等待批改 */
    private Integer isCorrect;

    /** 该题实际得分 */
    private Integer score;
}
