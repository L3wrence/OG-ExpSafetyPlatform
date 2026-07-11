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
@TableName("t_step_learning_record")
public class StepLearningRecord extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long stepId;
    private Long experimentId;
    private LocalDateTime completeTime;
}
