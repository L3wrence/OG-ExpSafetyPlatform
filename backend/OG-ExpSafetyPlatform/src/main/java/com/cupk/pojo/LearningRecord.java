package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//资源学习记录
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_learning_record")
public class LearningRecord extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;     //学生ID
    private Long resourceId;    //资源ID
    private Long experimentId;  //实验ID
    private BigDecimal progress; //学习进度
    private Integer durationSeconds; //累计学习时长
    private Integer finishFlag; //是否完成
    private LocalDateTime firstTime;    //开始学习时间
    private LocalDateTime lastTime;     //最后学习时间
}
