package com.hh.hhojbackendcommentservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hh.hhojbackendmodel.entity.CommentThumb;
import com.hh.hhojbackendmodel.entity.User;


/**
 * 帖子点赞服务
 *

 */
public interface CommentThumbService extends IService<CommentThumb> {

    /**
     * 点赞
     *
     * @param commentId
     * @param loginUser
     * @return
     */
    int docommentThumb(long commentId, User loginUser);

    /**
     * 帖子点赞（内部服务）
     *
     * @param userId
     * @param commentId
     * @return
     */
    int docommentThumbInner(long userId, long commentId);
}
