package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_hse_wrong_question")
public class HseWrongQuestion extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long questionId;
    private Long knowledgeId;
    private String knowledgePoint;
    private String riskType;
    private Integer wrongCount;
    private Integer correctStreak;
    private String masteryStatus;
    private LocalDateTime lastWrongTime;
}
