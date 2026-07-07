package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_report_rubric_item")
public class ReportRubricItem extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long experimentId;
    private String itemName;
    private String description;
    private Integer maxScore;
    private Integer orderNo;
}
