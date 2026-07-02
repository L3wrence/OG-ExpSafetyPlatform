package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * AI问答记录表 t_ai_chat_record
 * 记录AI使用情况
 */
@Data
@TableName("t_ai_chat_record")
public class AiChatRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 使用场景：SAFETY_QA / ERROR_EXPLAIN / REPORT_SUGGEST */
    private String scene;

    /** 用户提问/Prompt */
    private String question;

    /** AI生成的回答 */
    private String answer;

    /** 使用的AI工具名称 */
    private String toolName;

    /** 关联实验ID */
    private Long experimentId;

    /** 人工修改说明（课程要求必须记录） */
    private String manualRevision;

    /** 使用时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
}
