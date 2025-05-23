package com.hh.hhojbackendquestionservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hh.hhojbackendmodel.dto.question.QuestionQueryRequest;
import com.hh.hhojbackendmodel.entity.Question;
import com.hh.hhojbackendmodel.entity.User;
import com.hh.hhojbackendmodel.vo.HotQuestionVO;
import com.hh.hhojbackendmodel.vo.QuestionVO;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;

/**
* @author ybb
* @description 针对表【question(题目)】的数据库操作Service
* @createDate 2024-12-10 21:28:35
*/
public interface QuestionService extends IService<Question> {

    @Transactional
    boolean deleteQuestionAndSubmit(Long questionId, User user);

    /**
     * 校验
     *
     * @param question
     * @param add
     */
    void validQuestion(Question question, boolean add);

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest);


    /**
     * 获取题目封装
     *
     * @param question
     * @param loginUser
     * @return
     */
    QuestionVO getQuestionVO(Question question, User loginUser);

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request);

    Page<HotQuestionVO> listHotQuestions(QuestionQueryRequest questionQueryRequest);
}
