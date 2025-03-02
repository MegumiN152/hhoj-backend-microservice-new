package com.hh.hhojbackendjudgeservice.strategy;


import com.hh.hhojbackendmodel.codeSandBox.JudgeInfo;
import com.hh.hhojbackendmodel.dto.question.JudgeCase;
import com.hh.hhojbackendmodel.entity.Question;
import com.hh.hhojbackendmodel.entity.QuestionSubmit;
import lombok.Data;

import java.util.List;

/**
 * @author 黄昊
 * @version 1.0
 * 上下文（用于定义在策略中传递的参数）
 **/
@Data
public class JudgeContext {
    private JudgeInfo judgeInfo;

    private List<String> inputList;

    private List<String> outputList;

    private Question question;

    private QuestionSubmit questionSubmit;

    private List<JudgeCase> judgeCaseList;

    private String message;
}
