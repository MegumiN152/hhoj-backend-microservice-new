package com.hh.hhojbackendjudgeservice;


import com.hh.hhojbackendjudgeservice.strategy.DefaultJudgeStrategy;
import com.hh.hhojbackendjudgeservice.strategy.JavaLanguageJudgeStrategy;
import com.hh.hhojbackendjudgeservice.strategy.JudgeContext;
import com.hh.hhojbackendjudgeservice.strategy.JudgeStrategy;
import com.hh.hhojbackendmodel.codeSandBox.JudgeInfo;
import com.hh.hhojbackendmodel.entity.QuestionSubmit;
import com.hh.hhojbackendmodel.enums.QuestionsSubmitLanguageEnum;
import org.springframework.stereotype.Service;

/**
 * @author 黄昊
 * @version 1.0
 * 判题管理（简化调用）
 **/
@Service
public class JudgeManager {
    /**
     * 执行判题
     */
     JudgeInfo doJudge(JudgeContext judgeContext) {
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        String language = questionSubmit.getLanguage();
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        if (QuestionsSubmitLanguageEnum.JAVA.getValue().equals(language)) {
            // Java判题
            judgeStrategy = new JavaLanguageJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }
}
