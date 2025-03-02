package com.hh.hhojbackendcommentservice.controller;


import com.hh.hhojbackendcommentservice.manager.RedisLimiterManager;
import com.hh.hhojbackendcommentservice.service.CommentThumbService;
import com.hh.hhojbackendcommon.common.BaseResponse;
import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendcommon.common.ResultUtils;
import com.hh.hhojbackendmodel.dto.commentthumb.CommentThumbAddRequest;
import com.hh.hhojbackendmodel.entity.User;
import com.hh.hhojbackendserviceclient.exception.BusinessException;
import com.hh.hhojbackendserviceclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 评论点赞接口
 *
 */
@RestController
@RequestMapping("/comment_thumb")
@Slf4j
public class CommentThumbController {

    @Resource
    private CommentThumbService commentThumbService;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    /**
     * 点赞 / 取消点赞
     *
     * @param commentThumbAddRequest
     * @param request
     * @return resultNum 本次点赞变化数
     */
    @PostMapping("/")
    public BaseResponse<Integer> doThumb(@RequestBody CommentThumbAddRequest commentThumbAddRequest,
                                         HttpServletRequest request) {
        if (commentThumbAddRequest == null || commentThumbAddRequest.getCommentId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能点赞
        final User loginUser = userFeignClient.getLoginUser(request);
        long commentId = commentThumbAddRequest.getCommentId();
        redisLimiterManager.doRateLimitThumb(loginUser.getId().toString()+"_CommentThumb");
        int result = commentThumbService.docommentThumb(commentId, loginUser);
        return ResultUtils.success(result);
    }

}
