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
@TableName("t_teacher_certification")
public class TeacherCertification extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String school;
    private String employeeNo;
    private String educationEmail;
    private String status;
    private Long reviewerId;
    private String reviewComment;
    private LocalDateTime reviewTime;
}
