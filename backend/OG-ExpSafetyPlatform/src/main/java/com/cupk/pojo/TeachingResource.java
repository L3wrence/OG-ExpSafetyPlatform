package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

//教学资源
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_resource")
public class TeachingResource extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long experimentId;      //实验项目ID
    private String title;           //资源标题
    private String resourceType;    //资源类型
    private String url;             //外部链接
    private String filePath;        //文件路径
    private Long fileSize;          //文件大小
    private Integer requiredFlag;   //是否必学
    private Integer viewCount;      //浏览次数
    private Integer status;         //开放状态
    private Integer sort;           //展示排序
    private Long uploadUserId;      //上传人
}
