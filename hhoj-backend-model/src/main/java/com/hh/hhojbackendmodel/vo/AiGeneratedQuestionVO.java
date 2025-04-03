package com.hh.hhojbackendmodel.vo;

import cn.hutool.json.JSONUtil;
import com.hh.hhojbackendmodel.dto.question.JudgeCase;
import com.hh.hhojbackendmodel.dto.question.JudgeConfig;
import com.hh.hhojbackendmodel.entity.Question;
import com.hh.hhojbackendmodel.enums.QuestionTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;

import java.util.List;

/**
 * AI生成的题目信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiGeneratedQuestionVO {
    
    /**
     * 题目标题
     */
    private String title;
    
    /**
     * 题目内容
     */
    private String content;
    
    /**
     * 难度等级（0-简单，1-中等，2-困难）
     */
    private Integer difficulty;
    
    /**
     * 示例代码（Java）
     */
    private String sampleCode;
    
    /**
     * 判题用例
     */
    private List<JudgeCase> judgeCase;

    //转question
    public static Question toQuestion(AiGeneratedQuestionVO aiGeneratedQuestionVO,List<String> tags,Long userId) {
        if (aiGeneratedQuestionVO == null) {
            return null;
        }
        Question question = new Question();
        BeanUtils.copyProperties(aiGeneratedQuestionVO, question);
        question.setAnswer(aiGeneratedQuestionVO.getSampleCode());
        question.setUserId(userId);
        question.setType(QuestionTypeEnum.AI.getValue());
        if (tags!=null){
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        // 设置判题用例和配置
        List<JudgeCase> judgeCases = aiGeneratedQuestionVO.getJudgeCase();
        if (judgeCases != null && !judgeCases.isEmpty()) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCases));
        }
        // 创建默认判题配置
        JudgeConfig judgeConfig = new JudgeConfig();
        judgeConfig.setTimeLimit(1000l);  // 1秒
        judgeConfig.setMemoryLimit(256 * 1024 * 1024l);  // 256MB
        judgeConfig.setStackLimit(128 * 1024 * 1024l);  // 128MB
        question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        return question;
    }
}