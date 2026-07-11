package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//教学资源
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_resource")
public class TeachingResource extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;          //课程ID
    private Long experimentId;      //实验项目ID
    private String title;           //资源标题
    private String resourceType;    //资源类型
    private String businessCategory; //教学业务分类
    private String knowledgePoint;  //知识点
    private String riskType;        //风险类型
    private String tags;            //标签
    private String category;        //必学/拓展分类
    private String description;     //资源简介
    private String filePath;        //文件路径
    private String originalFilename; //原始文件名
    private String contentType;     //文件MIME类型
    private Long fileSize;          //文件大小
    private Integer requiredFlag;   //是否必学
    private String completionRule;  //完成规则
    private Integer minStudySeconds; //最少学习时长
    private Integer minProgress;    //最低进度
    private LocalDateTime openTime; //开放时间
    private LocalDateTime closeTime; //关闭时间
    private String openScope;       //开放范围
    private Integer invalidFlag;    //失效标记
    private LocalDateTime invalidCheckTime; //失效检查时间
    private Integer viewCount;      //浏览次数
    private Integer downloadCount;  //下载次数
    private Integer favoriteCount;  //收藏数
    private Integer likeCount;      //点赞数
    private Integer commentCount;   //评论数
    private BigDecimal ratingAvg;   //平均评分
    private Integer ratingCount;    //评分数
    private Integer status;         //开放状态
    private Integer sort;           //展示排序
    private Long uploadUserId;      //上传人
}
