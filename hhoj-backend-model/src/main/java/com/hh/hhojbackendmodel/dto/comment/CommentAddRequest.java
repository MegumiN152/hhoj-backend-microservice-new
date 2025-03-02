package com.hh.hhojbackendmodel.dto.comment;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建请求
 *

 */
@Data
public class CommentAddRequest implements Serializable {
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
    private static final long serialVersionUID = 1L;

}