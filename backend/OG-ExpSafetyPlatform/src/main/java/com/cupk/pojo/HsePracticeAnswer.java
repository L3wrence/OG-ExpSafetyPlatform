package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_hse_practice_answer")
public class HsePracticeAnswer extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long questionId;
    private Long knowledgeId;
    private Long experimentId;
    private String riskType;
    private String practiceType;
    private String studentAnswer;
    private Integer correctFlag;
    private Integer score;
}
