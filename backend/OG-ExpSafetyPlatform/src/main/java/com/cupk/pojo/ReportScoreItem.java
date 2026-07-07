package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_report_score_item")
public class ReportScoreItem extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reportScoreId;
    private Long rubricItemId;
    private BigDecimal score;
    private String comment;
}
