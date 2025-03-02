package com.hh.hhojbackendmodel.dto.commentthumb;

import lombok.Data;

import java.io.Serializable;

/**
 * 帖子点赞请求
 *

 */
@Data
public class CommentThumbAddRequest implements Serializable {

    /**
     * 评论 id
     */
    private Long commentId;

    private static final long serialVersionUID = 1L;
}