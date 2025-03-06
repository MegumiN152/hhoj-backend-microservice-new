package com.hh.hhojbackendjudgeservice.strategy;

import cn.hutool.json.JSONUtil;
import com.hh.hhojbackendmodel.codeSandBox.JudgeInfo;
import com.hh.hhojbackendmodel.dto.question.JudgeCase;
import com.hh.hhojbackendmodel.dto.question.JudgeConfig;
import com.hh.hhojbackendmodel.entity.Question;
import com.hh.hhojbackendmodel.enums.JudgeInfoMessageEnum;


import java.util.List;
import java.util.Optional;

/**
 * @author 黄昊
 * @version 1.0
 * 默认判题策略
 **/
public class DefaultJudgeStrategy implements JudgeStrategy {
    /**
     * 执行判题
     * @param judgeContext
     * @return
     */
    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        JudgeInfo judgeInfo = judgeContext.getJudgeInfo();
        List<String> inputList = judgeContext.getInputList();
        List<String> outputList = judgeContext.getOutputList();
        Question question = judgeContext.getQuestion();
        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();
        //代码执行实际的内存和时间
        Long memory = Optional.ofNullable(judgeInfo.getMemory()).orElse(0L);
        Long time = Optional.ofNullable(judgeInfo.getTime()).orElse(0L);
        //根据执行结果，判断题目的判题状态和信息
        JudgeInfoMessageEnum judgeInfoMessageEnum = JudgeInfoMessageEnum.ACCEPTED;
        JudgeInfo judgeInfoResponse = new JudgeInfo();
        judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
        judgeInfoResponse.setMemory(memory);
        judgeInfoResponse.setTime(time);
        if(judgeContext.getMessage()!=null &&judgeContext.getMessage().contains(JudgeInfoMessageEnum.COMPILE_ERROR.getValue())){
            judgeInfoMessageEnum = JudgeInfoMessageEnum.COMPILE_ERROR;
            judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResponse;
        }
        if (outputList.size() != inputList.size()) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
            judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResponse;
        }
        for (int i = 0; i < judgeCaseList.size(); i++) {
            JudgeCase judgeCase1 = judgeCaseList.get(i);
            if (!judgeCase1.getOutput().equals(outputList.get(i))) {
                judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
                judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
                return judgeInfoResponse;
            }
        }

        //获取题目中的预设的时间限制和内存限制
        String judgeConfigStr = question.getJudgeConfig();
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);
        Long timeLimit = judgeConfig.getTimeLimit();
        Long memoryLimit = judgeConfig.getMemoryLimit();
        if (memory > memoryLimit) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED;
            judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResponse;
        }
        if (time > timeLimit) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED;
            judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResponse;
        }
        judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
        return judgeInfoResponse;
    }
}
