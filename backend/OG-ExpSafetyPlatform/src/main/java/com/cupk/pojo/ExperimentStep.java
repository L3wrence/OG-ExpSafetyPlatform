package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

//实验项目步骤
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_experiment_step")
public class ExperimentStep extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long experimentId;      //实验项目ID
    private Integer stepNo;         //步骤序号
    private String title;           //步骤标题
    private String content;         //操作内容
    private String safetyTip;       //安全提示
    private String mediaType;        //媒体类型
    private String mediaFilePath;
    private String mediaOriginalFilename;
    private String mediaContentType;
    private Long mediaFileSize;
    private String flowchartData;    //流程图数据
    private Integer requiredFlag;   //是否必学
    private Integer estimatedMinutes; //预计时长
}
