package com.cupk.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cupk.common.BasePojo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_hse_question_favorite")
public class HseQuestionFavorite extends BasePojo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long questionId;
}
