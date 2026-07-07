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
@TableName("t_learning_task_record")
public class LearningTaskRecord extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long studentId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime completeTime;
    private String sourceType;
}
