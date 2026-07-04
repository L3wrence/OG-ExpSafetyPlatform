package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 考试记录表 t_exam_record
 * 记录每次考试的完整信息，与题目级答题明细表分离
 */
@Data
@TableName("t_exam_record")
public class ExamRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 学生ID（引用 t_user.id） */
    private Long studentId;

    /** 试卷ID（引用 t_exam_paper.id） */
    private Long paperId;
    private Long experimentId;

    /** 总得分（交卷后计算） */
    private Integer totalScore;

    /** 客观题得分（自动评分） */
    private Integer objectiveScore;

    /** 主观题得分（教师批改后） */
    private Integer subjectiveScore;

    /** 状态：IN_PROGRESS / SUBMITTED / REVIEWED */
    private String status;

    /** 是否通过（score >= pass_score） */
    private Integer passed;

    /** 开始答题时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date startTime;

    /** 交卷时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date submitTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
