package com.hh.hhojbackendmodel.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 评论表
 * @TableName comment
 */
@TableName(value ="comment")
@Data
public class Comment implements Serializable {
    /**
     * 
     */
    @TableId
    private Long id;

    /**
     * 发表评论的访客id
     */
    private Long userId;

    /**
     * 被评论的文章的id（可为空）
     */
    private Long questionId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 对别的评论发表的二级评论的id（可为空）
     */
    private Long beCommentId;
    /**
     * 点赞数
     */
    private Integer likeCount;
    /**
     * 回复数量
     */
    private Integer replyCount;
    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}