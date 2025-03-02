package com.hh.hhojbackendquestionservice.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendcommon.constant.CommonConstant;
import com.hh.hhojbackendcommon.constant.MqConstant;
import com.hh.hhojbackendcommon.utils.SqlUtils;
import com.hh.hhojbackendmodel.dto.questionsubmit.QuestionSubmitAddRequest;
import com.hh.hhojbackendmodel.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.hh.hhojbackendmodel.entity.Question;
import com.hh.hhojbackendmodel.entity.QuestionSubmit;
import com.hh.hhojbackendmodel.entity.User;
import com.hh.hhojbackendmodel.enums.QuestionSubmitStatusEnum;
import com.hh.hhojbackendmodel.enums.QuestionsSubmitLanguageEnum;
import com.hh.hhojbackendmodel.vo.*;
import com.hh.hhojbackendquestionservice.exception.BusinessException;
import com.hh.hhojbackendquestionservice.mapper.QuestionSubmitMapper;
import com.hh.hhojbackendquestionservice.mq.RabbitmqProducer;
import com.hh.hhojbackendquestionservice.service.QuestionService;
import com.hh.hhojbackendquestionservice.service.QuestionSubmitService;
import com.hh.hhojbackendserviceclient.service.JudgeFeignClient;
import com.hh.hhojbackendserviceclient.service.UserFeignClient;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ybb
 * @description 针对表【question_submit(题目提交)】的数据库操作Service实现
 * @createDate 2024-12-10 21:28:49
 */
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
        implements QuestionSubmitService {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    @Lazy
    private JudgeFeignClient judgeFeignClient;

    @Resource
    private RabbitmqProducer rabbitmqProducer;

    @Resource
    private QuestionSubmitMapper questionSubmitMapper;

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        String language = questionSubmitAddRequest.getLanguage();
        QuestionsSubmitLanguageEnum languageEnum = QuestionsSubmitLanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "暂不支持该编程语言："+language);
        }
        String code = questionSubmitAddRequest.getCode();
        Long questionId = questionSubmitAddRequest.getQuestionId();
        // 判断实体是否存在，根据类别获取实体
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 是否已点赞
        long userId = loginUser.getId();
        // 每个用户串行点赞
        // 锁必须要包裹住事务方法
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(userId);
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setCode(code);
        questionSubmit.setLanguage(language);
        // 设置初始状态
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        questionSubmit.setJudgeInfo("{}");
        boolean save = this.save(questionSubmit);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据插入失败");
        }
        rabbitmqProducer.sendMessage(MqConstant.EXCHANGE_NAME, MqConstant.NORMAL_ROUTING_KEY, String.valueOf(questionSubmit.getId()));
        return questionSubmit.getId();
    }


    /**
     * 获取查询包装类
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }
        String language = questionSubmitQueryRequest.getLanguage();
        Integer status = questionSubmitQueryRequest.getStatus();
        Long questionId = questionSubmitQueryRequest.getQuestionId();
        Long userId = questionSubmitQueryRequest.getUserId();
        String sortField = questionSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();

        queryWrapper.eq(StringUtils.isNotBlank(language), "language", language);
        queryWrapper.ne(QuestionSubmitStatusEnum.getEnumByValue(status) != null, "status", status);
        queryWrapper.ne(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser) {
        QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        //只有本人和管理员才能看见提交的代码
        Long userId1 = loginUser.getId();
        // 1. 关联查询用户信息
        Long userId = questionSubmitVO.getUserId();
        if (!userId1.equals(userId) && !userFeignClient.isAdmin(loginUser)) {
            questionSubmitVO.setCode(null);
        }
       if (userId!=null&&userId > 0){
           User user = userFeignClient.getById(userId);
           UserVO userVO = userFeignClient.getUserVO(user);
           questionSubmitVO.setUserVO(userVO);
       }
        //2. 关联查询题目信息
        Long questionId = questionSubmit.getQuestionId();
       if (questionId!=null&&questionId > 0){
           Question question = questionService.getById(questionId);
           QuestionVO questionVO = questionService.getQuestionVO(question, loginUser);
           questionSubmitVO.setQuestionVO(questionVO);
       }
        return questionSubmitVO;
    }

    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser) {
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        if (CollUtil.isEmpty(questionSubmitList)) {
            return questionSubmitVOPage;
        }
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionSubmitList.stream().map(QuestionSubmit::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userFeignClient.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 关联查询题目信息
        Set<Long> questionIdSet = questionSubmitList.stream().map(QuestionSubmit::getQuestionId).collect(Collectors.toSet());
        Map<Long, List<Question>> questionIdQuestionListMap = questionService.listByIds(questionIdSet).stream()
                .collect(Collectors.groupingBy(Question::getId));
        // 填充信息
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream().map(questionSubmit -> {
            QuestionSubmitVO questionSubmitVO = getQuestionSubmitVO(questionSubmit,loginUser);
            Long userId = questionSubmit.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionSubmitVO.setUserVO(userFeignClient.getUserVO(user));

            Long questionId = questionSubmit.getQuestionId();
            Question question = null;
            if (questionIdQuestionListMap.containsKey(questionId)) {
                question = questionIdQuestionListMap.get(questionId).get(0);
            }
            questionSubmitVO.setQuestionVO(questionService.getQuestionVO(question, loginUser));
            return questionSubmitVO;
        }).collect(Collectors.toList());
        questionSubmitVOPage.setRecords(questionSubmitVOList);
        return questionSubmitVOPage;
    }
    @Override
    public Page<MyQuestionSubmitVO> getMyQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser) {
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<MyQuestionSubmitVO> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        if (CollUtil.isEmpty(questionSubmitList)) {
            return questionSubmitVOPage;
        }
        // 1. 关联查询用户信息
        User loginUserNew = userFeignClient.getById(loginUser.getId());
        // 2. 关联查询题目信息
        Set<Long> questionIdSet = questionSubmitList.stream().map(QuestionSubmit::getQuestionId).collect(Collectors.toSet());
        Map<Long, List<Question>> questionIdQuestionListMap = questionService.listByIds(questionIdSet).stream()
                .collect(Collectors.groupingBy(Question::getId));
        // 填充信息
        List<MyQuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream()
                .limit(5)
        .map(questionSubmit -> {
            MyQuestionSubmitVO myQuestionSubmitVO = new MyQuestionSubmitVO();
            Long questionId = questionSubmit.getQuestionId();
            Question question = null;
            if (questionIdQuestionListMap.containsKey(questionId)) {
                question = questionIdQuestionListMap.get(questionId).get(0);
            }
            myQuestionSubmitVO.setTitle(question.getTitle());
            myQuestionSubmitVO.setCreateTime(questionSubmit.getCreateTime());
            myQuestionSubmitVO.setStatus(questionSubmit.getStatus());
            myQuestionSubmitVO.setLanguage(questionSubmit.getLanguage());
            myQuestionSubmitVO.setId(questionSubmit.getQuestionId());
            return myQuestionSubmitVO;
        }).collect(Collectors.toList());
        questionSubmitVOPage.setRecords(questionSubmitVOList);
        return questionSubmitVOPage;
    }
    @Override
    public UserStatsVO getUserStats(Long id) {
        UserStatsVO userStatsVO = new UserStatsVO();
        //查询用户提交数
        LambdaQueryWrapper<QuestionSubmit> questionSubmitLambdaQueryWrapper = new LambdaQueryWrapper<>();
        questionSubmitLambdaQueryWrapper.eq(QuestionSubmit::getUserId, id);
        int submitCount = (int) this.count(questionSubmitLambdaQueryWrapper);
        //查询用户成功提交数
        questionSubmitLambdaQueryWrapper.eq(QuestionSubmit::getStatus, QuestionSubmitStatusEnum.SUCCEED.getValue());
        questionSubmitLambdaQueryWrapper.groupBy(QuestionSubmit::getQuestionId);
        int solvedCount = (int) this.count(questionSubmitLambdaQueryWrapper);
        //计算通过率,保留两位小数
        double passRate= submitCount>0 ?(double)solvedCount/submitCount*100 :0;
        userStatsVO.setSubmitCount(submitCount);
        userStatsVO.setSolvedCount(solvedCount);
        userStatsVO.setPassRate(passRate);
        return userStatsVO;
    }

    @Override
    public List<UserLeaderboardVO> getLeaderboard() {
        return questionSubmitMapper.getLeaderBoard(5);
    }
}




