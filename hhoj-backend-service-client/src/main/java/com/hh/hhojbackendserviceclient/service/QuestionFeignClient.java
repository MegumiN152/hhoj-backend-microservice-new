package com.hh.hhojbackendserviceclient.service;

import com.hh.hhojbackendmodel.dto.questionsubmit.QuestionSubmitQueryDTO;
import com.hh.hhojbackendmodel.entity.Question;
import com.hh.hhojbackendmodel.entity.QuestionSubmit;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author ybb
 * @description 针对表【question(题目)】的数据库操作Service
 * @createDate 2024-12-10 21:28:35
 */
@FeignClient(name = "hhoj-backend-question-service", path = "/api/question/inner")
public interface QuestionFeignClient {
    /**
     * 根据查询条件批量获取题目提交列表
     *
     * @param questionSubmitQueryDTO
     * @return
     */
    @PostMapping("/question_submit/list")
    List<QuestionSubmit> list(@RequestBody QuestionSubmitQueryDTO questionSubmitQueryDTO);

    /**
     * 根据查询条件获取题目
     *
     * @param questionId
     * @return
     */
    @GetMapping("/get/one")
    Question getOne(@RequestParam("questionId") long questionId);

    /**
     * 根据id获取题目提交记录
     *
     * @param questionSubmitId
     * @return
     */
    @GetMapping("/question_submit/get/id")
    QuestionSubmit getQuestionSubmitById(@RequestParam("questionSubmitId") long questionSubmitId);

    @GetMapping("/get/id")
    Question getQuestionById(@RequestParam("questionId") long questionId);

    @PostMapping("/question_submit/update/id")
    Boolean updateQuestionSubmitById(@RequestBody QuestionSubmit questionSubmit);


    @PostMapping("/question/update/id")
    Boolean updateQuestionById(@RequestBody Question question);

}
