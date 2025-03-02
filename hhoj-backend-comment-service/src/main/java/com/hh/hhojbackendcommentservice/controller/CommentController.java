package com.hh.hhojbackendcommentservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hh.hhojbackendcommentservice.exception.BusinessException;
import com.hh.hhojbackendcommentservice.manager.RedisLimiterManager;
import com.hh.hhojbackendcommentservice.service.CommentService;
import com.hh.hhojbackendcommentservice.utils.WordUtils;
import com.hh.hhojbackendcommon.annotation.AuthCheck;
import com.hh.hhojbackendcommon.common.BaseResponse;
import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendcommon.common.ResultUtils;
import com.hh.hhojbackendcommon.constant.UserConstant;
import com.hh.hhojbackendmodel.dto.comment.CommentAddRequest;
import com.hh.hhojbackendmodel.dto.comment.CommentDeleteRequest;
import com.hh.hhojbackendmodel.entity.Comment;
import com.hh.hhojbackendmodel.entity.User;
import com.hh.hhojbackendmodel.vo.CommentVO;
import com.hh.hhojbackendserviceclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 黄昊
 * @version 1.0
 **/
@RestController
@RequestMapping("/")
@Slf4j
public class CommentController {
    @Resource
    private UserFeignClient userFeignClient;
    @Resource
    private CommentService commentService;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<Long> addComment(@RequestBody CommentAddRequest commentAddRequest, HttpServletRequest request) {
        if (commentAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        User loginUser = userFeignClient.getLoginUser(request);
        if(!WordUtils.containsBadWords(commentAddRequest.getContent())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"包含敏感词汇");
        }
        if (!(loginUser.getId().equals(commentAddRequest.getUserId()))) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //添加评论限流
        redisLimiterManager.doRateLimit("addComment"+loginUser.getId());
        Long commentId = commentService.addComment(commentAddRequest);
        return ResultUtils.success(commentId);
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<Boolean> deleteComment(@RequestBody CommentDeleteRequest commentDeleteRequest, HttpServletRequest request) {
        if (commentDeleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        User loginUser = userFeignClient.getLoginUser(request);
        Comment comment = commentService.getById(commentDeleteRequest.getId());
        if (comment == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论不存在");
        }
        Boolean result = commentService.deleteComment(comment, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "删除失败");
        }
        return ResultUtils.success(result);
    }

//    @GetMapping("/list/question/{questionId}")
//    public BaseResponse<List<CommentVO>> listQuestionComments(@PathVariable("questionId") Long questionId) {
//        if (questionId == null || questionId <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        List<CommentVO> commentVOList = commentService.listCommentsByQuestionId(questionId);
//        return ResultUtils.success(commentVOList);
//    }
    @GetMapping("/list/question/{questionId}")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<Page<CommentVO>> listQuestionComments(
            @PathVariable("questionId") Long questionId,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "3") Long pageSize,
            @RequestParam(defaultValue = "latest") String sortType,HttpServletRequest request) {
        if (questionId == null || questionId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request);
        Page<CommentVO> commentPage = commentService.listQuestionComments(questionId, current, pageSize, sortType,loginUser.getId());
        return ResultUtils.success(commentPage);
    }
    @GetMapping("/list/replies/{commentId}")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<List<CommentVO>> listCommentReplies(@PathVariable("commentId") Long commentId) {
        if (commentId == null || commentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<CommentVO> commentVOList = commentService.listCommentReplies(commentId);
        return ResultUtils.success(commentVOList);
    }
    @PostMapping("/like/{id}")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<Boolean> likeComment(@PathVariable("id") Long commentId, HttpServletRequest request) {
        if (commentId == null || commentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request);
        boolean result = commentService.likeComment(commentId, loginUser.getId());
        return ResultUtils.success(result);
    }
    @PostMapping("/delete/{id}")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<Boolean> deleteComment(@PathVariable("id") Long commentId, HttpServletRequest request) {
        if (commentId == null || commentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request);
        boolean result = commentService.removeComment(commentId, loginUser.getId());
        return ResultUtils.success(result);
    }

}
