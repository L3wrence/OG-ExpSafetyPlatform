package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_report_template")
public class ReportTemplate extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long experimentId;
    private String title;
    private String schemaJson;
    private Integer status;
}
