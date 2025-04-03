package com.hh.hhojbackendquestionservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hh.hhojbackendmodel.dto.questionsubmit.QuestionSubmitAddRequest;
import com.hh.hhojbackendmodel.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.hh.hhojbackendmodel.entity.Question;
import com.hh.hhojbackendmodel.entity.QuestionSubmit;
import com.hh.hhojbackendmodel.entity.User;
import com.hh.hhojbackendmodel.vo.MyQuestionSubmitVO;
import com.hh.hhojbackendmodel.vo.QuestionSubmitVO;
import com.hh.hhojbackendmodel.vo.UserLeaderboardVO;
import com.hh.hhojbackendmodel.vo.UserStatsVO;

import java.util.List;


/**
* @author ybb
* @description 针对表【question_submit(题目提交)】的数据库操作Service
* @createDate 2024-12-10 21:28:49
*/
public interface QuestionSubmitService extends IService<QuestionSubmit> {

    long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser);

    /**
     * 获取查询条件
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest);


    /**
     * 获取题目封装
     *
     * @param questionSubmit
     * @param loginUser
     * @return
     */
    QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser, User user, Question question);

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param loginUser
     * @return
     */
    Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionPage, User loginUser);

    Page<MyQuestionSubmitVO> getMyQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser);

    UserStatsVO getUserStats(Long id);

    List<UserLeaderboardVO> getLeaderboard();
}
