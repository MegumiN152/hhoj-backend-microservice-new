package com.hh.hhojbackendcommentservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hh.hhojbackendcommentservice.mapper.CommentThumbMapper;
import com.hh.hhojbackendcommentservice.service.CommentService;
import com.hh.hhojbackendcommentservice.service.CommentThumbService;
import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendmodel.entity.Comment;
import com.hh.hhojbackendmodel.entity.CommentThumb;
import com.hh.hhojbackendmodel.entity.User;
import com.hh.hhojbackendserviceclient.exception.BusinessException;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 帖子点赞服务实现
 *

 */
@Service
public class CommentThumbServiceImpl extends ServiceImpl<CommentThumbMapper, CommentThumb>
        implements CommentThumbService {

    @Resource
    private CommentService commentService;

    /**
     * 点赞
     *
     * @param commentId
     * @param loginUser
     * @return
     */
    @Override
    public int docommentThumb(long commentId, User loginUser) {
        // 判断实体是否存在，根据类别获取实体
        Comment comment = commentService.getById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 是否已点赞
        long userId = loginUser.getId();
        // 每个用户串行点赞
        // 锁必须要包裹住事务方法
        CommentThumbService commentThumbService = (CommentThumbService) AopContext.currentProxy();
        synchronized (String.valueOf(userId).intern()) {
            return commentThumbService.docommentThumbInner(userId, commentId);
        }
    }

    /**
     * 封装了事务的方法
     *
     * @param userId
     * @param commentId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int docommentThumbInner(long userId, long commentId) {
        CommentThumb commentThumb = new CommentThumb();
        commentThumb.setUserId(userId);
        commentThumb.setCommentId(commentId);
        QueryWrapper<CommentThumb> thumbQueryWrapper = new QueryWrapper<>(commentThumb);
        CommentThumb oldcommentThumb = this.getOne(thumbQueryWrapper);
        boolean result;
        // 已点赞
        if (oldcommentThumb != null) {
            result = this.remove(thumbQueryWrapper);
            if (result) {
                // 点赞数 - 1
                result = commentService.update()
                        .eq("id", commentId)
                        .gt("likeCount", 0)
                        .setSql("likeCount = likeCount - 1")
                        .update();
                return result ? -1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        } else {
            // 未点赞
            result = this.save(commentThumb);
            if (result) {
                // 点赞数 + 1
                result = commentService.update()
                        .eq("id", commentId)
                        .setSql("likeCount = likeCount + 1")
                        .update();
                return result ? 1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        }
    }

}




