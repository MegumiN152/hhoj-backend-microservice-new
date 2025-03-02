package com.hh.hhojbackendjudgeservice;

import cn.hutool.json.JSONUtil;
import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendjudgeservice.codesandbox.CodeSandbox;
import com.hh.hhojbackendjudgeservice.codesandbox.CodeSandboxFactory;
import com.hh.hhojbackendjudgeservice.codesandbox.CodeSandboxProxy;
import com.hh.hhojbackendjudgeservice.exception.BusinessException;
import com.hh.hhojbackendjudgeservice.strategy.JudgeContext;
import com.hh.hhojbackendmodel.codeSandBox.ExecuteCodeRequest;
import com.hh.hhojbackendmodel.codeSandBox.ExecuteCodeResponse;
import com.hh.hhojbackendmodel.codeSandBox.JudgeInfo;
import com.hh.hhojbackendmodel.dto.question.JudgeCase;
import com.hh.hhojbackendmodel.entity.Question;
import com.hh.hhojbackendmodel.entity.QuestionSubmit;
import com.hh.hhojbackendmodel.enums.JudgeInfoMessageEnum;
import com.hh.hhojbackendmodel.enums.QuestionSubmitStatusEnum;
import com.hh.hhojbackendserviceclient.service.QuestionFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 黄昊
 * @version 1.0
 **/
@Service
public class JudgeServiceImpl implements JudgeService {
    @Value("${codesandbox.type}")
    private String type;
    @Resource
    private QuestionFeignClient questionFeignClient;

    @Resource
    private JudgeManager judgeManager;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        QuestionSubmit questionSubmit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "找不到对应的题目提交信息");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionFeignClient.getQuestionById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        //如果不为等待状态,就不用重复执行了
        if (questionSubmit.getStatus().equals(QuestionSubmitStatusEnum.JUDGING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在判题中");
        }
        //更改判题状态为”判题中“，防止重复执行
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.JUDGING.getValue());
        boolean update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        //调用沙箱，获取执行结果
        CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
        codeSandbox = new CodeSandboxProxy(codeSandbox);
        String language = questionSubmit.getLanguage();
        String code = questionSubmit.getCode();
        String judgeCase = question.getJudgeCase();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCase, JudgeCase.class);
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .language(language)
                .inputList(inputList)
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        List<String> outputList = executeCodeResponse.getOutputList();
        //设置判题上下文
        JudgeContext judgeContext = new JudgeContext();
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        judgeContext.setInputList(inputList);
        judgeContext.setOutputList(outputList);
        judgeContext.setQuestion(question);
        judgeContext.setJudgeCaseList(judgeCaseList);
        judgeContext.setQuestionSubmit(questionSubmit);
        //如果判题完的信息不为空则设置
       if (StringUtils.isNotBlank(executeCodeResponse.getMessage())){
           judgeContext.setMessage(executeCodeResponse.getMessage());
       }
        //启用默认判题策略
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);
        //更改判题状态为”判题中“，防止重复执行
        questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        //如果返回的判题信息中不是ac，则将状态设置为3
        if (!judgeInfo.getMessage().equals(JudgeInfoMessageEnum.ACCEPTED.getValue())) {
            questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
        } else {
            //设置question的通过数加1
            question.setAcceptedNum( question.getAcceptedNum() + 1);
            questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        }
        //不管成功还是失败，设置题目的提交数加1
        question.setSubmitNum(question.getSubmitNum() + 1);
        //更新题目信息
        questionFeignClient.updateQuestionById(question);
        //设置判题信息
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return questionFeignClient.getQuestionSubmitById(questionSubmitId);
    }
}
