package com.hh.hhojbackendcommentservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hh.hhojbackendcommentservice.exception.BusinessException;
import com.hh.hhojbackendcommentservice.mapper.CommentMapper;
import com.hh.hhojbackendcommentservice.mapper.CommentThumbMapper;
import com.hh.hhojbackendcommentservice.service.CommentService;
import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendmodel.dto.comment.CommentAddRequest;
import com.hh.hhojbackendmodel.entity.Comment;
import com.hh.hhojbackendmodel.entity.CommentThumb;
import com.hh.hhojbackendmodel.entity.User;
import com.hh.hhojbackendmodel.vo.CommentVO;
import com.hh.hhojbackendmodel.vo.UserVO;
import com.hh.hhojbackendserviceclient.service.UserFeignClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ybb
 * @description 针对表【comment(评论表)】的数据库操作Service实现
 * @createDate 2024-12-19 23:56:18
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
        implements CommentService {
    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private CommentThumbMapper commentThumbMapper;

    @Override
    public Long addComment(CommentAddRequest commentAddRequest) {
        Long userId = commentAddRequest.getUserId();
        Long questionId = commentAddRequest.getQuestionId();
        String content = commentAddRequest.getContent();
        Long beCommentId = commentAddRequest.getBeCommentId();
        if (content == null || content.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        }
        if (content.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论内容长度不合法");
        }
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setQuestionId(questionId);
        comment.setContent(content);
        comment.setBeCommentId(beCommentId);
        boolean save = this.save(comment);
        //通过questionId拿到评论map
        Map<Long, Comment> commentMap = this.list(new QueryWrapper<Comment>().eq("questionId", questionId)).stream().collect(Collectors.toMap(Comment::getId, c -> c));
        // 如果是回复评论，更新父评论的回复数
        if (comment.getBeCommentId() != null) {
            // 查找原始父评论（最顶层的父评论）
            Long rootParentId = findRootParentId(comment.getBeCommentId(), commentMap);

            // 更新原始父评论的回复数
            Comment rootParent = this.getById(rootParentId);
            if (rootParent != null) {
                rootParent.setReplyCount(rootParent.getReplyCount() + 1);
                this.updateById(rootParent);
            }
        }
        if (!save) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论失败");
        }
        return comment.getId();
    }

    /**
     * 查找评论的根父评论ID
     */
    private Long findRootParentId(Long commentId, Map<Long, Comment> commentMap) {
        Comment comment = commentMap.get(commentId);
        if (comment == null || comment.getBeCommentId() == null) {
            return commentId;
        }
        return findRootParentId(comment.getBeCommentId(), commentMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteComment(Comment delcomment, User loginUser) {
        Long commentId = delcomment.getId();
        Long questionId = delcomment.getQuestionId();
        // 1. 获取要删除的评论
        Comment comment = this.getById(commentId);
        if (comment == null) {
            return false;
        }

        // 2. 查找所有需要删除的评论ID（包括当前评论及其所有子孙评论）
        Set<Long> toDeleteIds = new HashSet<>();
        toDeleteIds.add(commentId);
        findAllChildComments(commentId, toDeleteIds, questionId);
        // 3. 删除评论本身（逻辑删除）
        return this.removeByIds(toDeleteIds);
    }

    @Override
    public List<CommentVO> listCommentsByQuestionId(Long questionId) {
        // 1. 获取该问题下的所有评论
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("questionId", questionId);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByDesc("createTime");
        List<Comment> commentList = this.list(queryWrapper);

        if (CollectionUtils.isEmpty(commentList)) {
            return new ArrayList<>();
        }

        // 2. 获取所有评论用户id
        Set<Long> userIds = commentList.stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());

        // 3. 获取用户信息
        Map<Long, UserVO> userVOMap = userFeignClient.listByIds(userIds).stream()
                .map(user -> userFeignClient.getUserVO(user)
                )
                .collect(Collectors.toMap(UserVO::getId, userVO -> userVO));

        // 4. 转换成 VO
        List<CommentVO> commentVOList = commentList.stream()
                .map(comment -> {
                    CommentVO commentVO = new CommentVO();
                    BeanUtils.copyProperties(comment, commentVO);
                    // 设置用户信息
                    commentVO.setUserVO(userVOMap.get(comment.getUserId()));
                    return commentVO;
                })
                .collect(Collectors.toList());

        // 5. 构建评论树
        return buildCommentTree(commentVOList);
    }

    /**
     * 构建评论树
     */
    private List<CommentVO> buildCommentTree(List<CommentVO> commentVOList) {
        // 1. 创建 id -> 评论 的映射，方便查找
        Map<Long, CommentVO> commentMap = commentVOList.stream()
                .collect(Collectors.toMap(CommentVO::getId, commentVO -> commentVO));

        // 2. 存储所有顶级评论
        List<CommentVO> rootComments = new ArrayList<>();

        // 3. 遍历所有评论，将其放入对应父评论的 children 中
        for (CommentVO commentVO : commentVOList) {
            Long beCommentId = commentVO.getBeCommentId();
            if (beCommentId == null) {
                // 顶级评论
                rootComments.add(commentVO);
            } else {
                // 子评论，找到父评论，加入其 children 列表
                CommentVO parentComment = commentMap.get(beCommentId);
                if (parentComment != null) {
                    if (parentComment.getChildren() == null) {
                        parentComment.setChildren(new ArrayList<>());
                    }
                    parentComment.getChildren().add(commentVO);
                }
            }
        }

        // 4. 对所有评论的 children 进行排序（按创建时间倒序）
        sortCommentChildren(rootComments);

        return rootComments;
    }

    /**
     * 递归对评论的子评论进行排序
     */
    private void sortCommentChildren(List<CommentVO> commentVOList) {
        if (CollectionUtils.isEmpty(commentVOList)) {
            return;
        }

        for (CommentVO commentVO : commentVOList) {
            if (commentVO.getChildren() != null) {
                // 按创建时间倒序排序
                commentVO.setChildren(commentVO.getChildren().stream()
                        .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                        .collect(Collectors.toList()));
                // 递归排序子评论的子评论
                sortCommentChildren(commentVO.getChildren());
            }
        }
    }

    /**
     * 递归查找所有子孙评论的ID
     */
    private void findAllChildComments(Long commentId, Set<Long> toDeleteIds, Long questionId) {
        // 查找所有直接引用该评论的子评论
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("beCommentId", commentId);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.eq("questionId", questionId);
        List<Comment> childComments = this.list(queryWrapper);

        for (Comment childComment : childComments) {
            // 将子评论ID加入待删除集合
            toDeleteIds.add(childComment.getId());
            // 继续递归查找该子评论的子评论
            findAllChildComments(childComment.getId(), toDeleteIds, questionId);
        }
    }

    @Override
    public Page<CommentVO> listQuestionComments(long questionId, long current, long pageSize, String sortType,Long userId) {
        // 构建查询条件
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("questionId", questionId)
                .isNull("beCommentId");

        // 设置排序
        if ("hot".equals(sortType)) {
            queryWrapper.orderByDesc("likeCount", "createTime");
        } else {
            queryWrapper.orderByDesc("createTime");
        }

        // 执行分页查询
        List<Comment> commentList = this.list(new QueryWrapper<Comment>().eq("questionId", questionId));
        if (commentList.isEmpty()) {
            return new Page<>();
        }
        // 2. 获取所有评论用户的信息
        Set<Long> userIds = commentList.stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userFeignClient.listByIds(userIds).stream()
                .map(user -> userFeignClient.getUserVO(user))
                .collect(Collectors.toMap(UserVO::getId, userVO -> userVO));

        // 2. 已登录，获取用户点赞状态
        Map<Long, Boolean> commentIdHasThumbMap = new HashMap<>();
        //获取关于这道题的commentIdSet集合
        Set<Long> commentSet = commentList.stream().map(Comment::getId).collect(Collectors.toSet());
        // 获取点赞
        QueryWrapper<CommentThumb> commentThumbQueryWrapper = new QueryWrapper<>();
        commentThumbQueryWrapper.in("commentId", commentSet);
        commentThumbQueryWrapper.eq("userId", userId);
        List<CommentThumb> commentCommentThumbList = commentThumbMapper.selectList(commentThumbQueryWrapper);
        commentCommentThumbList.forEach(commentCommentThumb -> commentIdHasThumbMap.put(commentCommentThumb.getCommentId(), true));
        // 3. 转换为VO并填充用户信息
        List<CommentVO> commentVOList = commentList.stream().map(reply -> {
            CommentVO replyVO = new CommentVO();
            BeanUtils.copyProperties(reply, replyVO);
            replyVO.setUserVO(userVOMap.get(reply.getUserId()));
            //设置用户是否点赞
            replyVO.setHasThumb(commentIdHasThumbMap.getOrDefault(reply.getId(), false));
            return replyVO;
        }).collect(Collectors.toList());
        List<CommentVO> commentVOS = buildCommentTree(commentVOList);
        //将commentVOS转成page
        Page<CommentVO> commentVOPage = new Page<>(current, pageSize);
        commentVOPage.setRecords(commentVOS);
        commentVOPage.setTotal(commentVOList.size());
        return commentVOPage;
    }

    @Override
    public List<CommentVO> listCommentReplies(long commentId) {
//        // 查询回复列表
//        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("beCommentId", commentId)
//                .orderByDesc("createTime");
//
//        List<Comment> replies = this.list(queryWrapper);
//
//        if (CollectionUtils.isEmpty(replies)) {
//            return new ArrayList<>();
//        }
        Comment fatherComment = this.getById(commentId);
        Long questionId = fatherComment.getQuestionId();
        List<Comment> commentList = this.list(new QueryWrapper<Comment>().eq("questionId", questionId));
        if (commentList==null){
            return new ArrayList<>();
        }
        // 2. 获取所有评论用户的信息
        Set<Long> userIds = commentList.stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userFeignClient.listByIds(userIds).stream()
                .map(user -> userFeignClient.getUserVO(user))
                .collect(Collectors.toMap(UserVO::getId, userVO -> userVO));
        // 3. 转换为VO并填充用户信息
        List<CommentVO> commentVOList = commentList.stream().map(reply -> {
            CommentVO replyVO = new CommentVO();
            BeanUtils.copyProperties(reply, replyVO);
            replyVO.setUserVO(userVOMap.get(reply.getUserId()));
            return replyVO;
        }).collect(Collectors.toList());
        List<CommentVO> commentVOS = buildCommentTree(commentVOList);
        Map<Long, CommentVO> collect = commentVOS.stream().collect(Collectors.toMap(CommentVO::getId, c -> c));
        return collect.get(commentId).getChildren();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean likeComment(long commentId, long userId) {
        Comment comment = this.getById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // TODO: 这里可以添加点赞记录表，防止重复点赞
        comment.setLikeCount(comment.getLikeCount() + 1);
        return this.updateById(comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeComment(long commentId, long userId) {
        Comment comment = this.getById(commentId);
        Long questionId = comment.getQuestionId();
        Map<Long, Comment> commentMap = this.list(new QueryWrapper<Comment>().eq("questionId", questionId)).stream().collect(Collectors.toMap(Comment::getId, c -> c));
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 校验权限
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        Long rootParentId = null;
        if (comment.getBeCommentId() != null) {
            rootParentId = findRootParentId(comment.getBeCommentId(), commentMap);
        }
        // 查找所有需要删除的评论ID
        Set<Long> toDeleteIds = new HashSet<>();
        toDeleteIds.add(commentId);
        findAllChildComments(commentId, toDeleteIds, comment.getQuestionId());
        boolean success = this.removeByIds(toDeleteIds);
        // 更新原始父评论的回复数
        if (success && rootParentId != null) {
            Comment rootParent = this.getById(rootParentId);
            if (rootParent != null) {
                // 重新计算回复数
                long replyCount = this.count(new QueryWrapper<Comment>()
                        .eq("beCommentId", rootParentId));
                rootParent.setReplyCount((int) replyCount);
                this.updateById(rootParent);
            }
        }
        return success;
    }
}




