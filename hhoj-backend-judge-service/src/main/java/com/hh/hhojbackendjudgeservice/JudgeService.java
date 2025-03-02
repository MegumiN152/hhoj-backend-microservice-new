package com.hh.hhojbackendjudgeservice;


import com.hh.hhojbackendmodel.entity.QuestionSubmit;

/**
 * @author 黄昊
 * @version 1.0
 **/
public interface JudgeService {
    public QuestionSubmit doJudge(long questionSubmitId);
}
