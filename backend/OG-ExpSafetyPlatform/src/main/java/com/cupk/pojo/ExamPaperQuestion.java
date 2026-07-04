package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 试卷-题目关联表 t_exam_paper_question
 * 实现试卷与题目的多对多关系，支持题目复用和组卷版本追溯
 */
@Data
@TableName("t_exam_paper_question")
public class ExamPaperQuestion {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 试卷ID（引用 t_exam_paper.id） */
    private Long paperId;

    /** 题目ID（引用 t_question.id） */
    private Long questionId;

    /** 该题在本试卷中的分值（可覆盖题目默认分值） */
    private Integer score;

    /** 题目在试卷中的排序号 */
    private Integer orderNum;
}
