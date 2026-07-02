package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 推荐记录表 t_recommend_record
 * 记录每次推荐的结果和理由，便于分析推荐效果
 */
@Data
@TableName("t_recommend_record")
public class RecommendRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 学生ID */
    private Long studentId;

    /** 关联实验ID */
    private Long experimentId;

    /** 推荐资源ID（引用 t_resource.id） */
    private Long resourceId;

    /** 推荐总分（0-100） */
    private BigDecimal totalScore;

    /** 打分明细JSON：{knowledgeMatch, errorRelevance, newness, popularity, difficultyMatch} */
    private String scoreBreakdown;

    /** 推荐理由（如"因为你XX知识点有2道错题..."） */
    private String reason;

    /** 学生是否点击查看了该推荐 */
    private Integer clicked;

    /** 推荐时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
}
