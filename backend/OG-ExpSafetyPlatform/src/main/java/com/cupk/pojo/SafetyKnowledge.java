package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_safety_knowledge")
public class SafetyKnowledge extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long experimentId;      //实验项目ID
    private String category;        //HSE分类
    private String knowledgePoint;  //知识点
    private String riskType;        //风险类型
    private String content;         //知识内容
    private Long relatedStepId;     //关联步骤
    private Long referenceResourceId; //参考资源
    private Integer emergencyFlag;  //是否应急处置
    private Integer status;         //开放状态
}
