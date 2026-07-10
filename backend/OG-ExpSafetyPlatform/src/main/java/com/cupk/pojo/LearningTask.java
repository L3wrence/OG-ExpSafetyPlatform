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
@TableName("t_learning_task")
public class LearningTask extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long experimentId;
    private String taskName;
    private String taskType;
    private Long targetResourceId;
    private Long targetPaperId;
    private Long prerequisiteTaskId;
    private Integer requiredFlag;
    private Integer sort;
    private LocalDateTime openTime;
    private LocalDateTime deadline;
    private String completionRule;
    private Integer status;
}
