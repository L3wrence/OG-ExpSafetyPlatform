package com.cupk.dto.question;

import lombok.Data;

/**
 * 题目创建/编辑请求DTO
 */
@Data
public class QuestionCreateDTO {
    /** 题型：SINGLE / MULTIPLE / JUDGE / SHORT_ANSWER */
    private String type;

    /** 题目内容 */
    private String content;

    /** 选项JSON */
    private String options;

    /** 正确答案 */
    private String answer;

    /** 默认分值 */
    private Integer score;

    /** 答案解析 */
    private String analysis;

    /** 知识点名称 */
    private String knowledgePoint;

    /** 难度 */
    private String difficulty;

    /** 关联课程ID */
    private Long courseId;
}
