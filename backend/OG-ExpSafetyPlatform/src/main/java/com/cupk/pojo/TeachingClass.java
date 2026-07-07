package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_teaching_class")
public class TeachingClass extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private String className;
    private Long teacherId;
    private Long assistantId;
    private String adminClass;
    private String semester;
    private Integer status;
}
