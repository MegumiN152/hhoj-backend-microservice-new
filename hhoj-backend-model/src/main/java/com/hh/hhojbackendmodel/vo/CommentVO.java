package com.hh.hhojbackendmodel.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 评论VO表
 *
 * @TableName comment
 */
@Data
public class CommentVO implements Serializable {
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
     * 回复数量
     */
    private Integer replyCount;
    /**
     * 点赞数量
     */
    private Integer likeCount;

    /**
     * 是否已点赞
     */
    private Boolean hasThumb;

    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 脱敏后的用户对象
     */
    private UserVO userVO;

    /**
     * 子评论
     */
    private List<CommentVO> children;
    /**
     * 是否展开 默认不
     */
    private Boolean showReplies = false;


    private static final long serialVersionUID = 1L;
}