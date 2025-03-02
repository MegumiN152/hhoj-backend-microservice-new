package com.hh.hhojbackendjudgeservice.strategy;


import com.hh.hhojbackendmodel.codeSandBox.JudgeInfo;

/**
 * @author 黄昊
 * @version 1.0
 * 判题策略
 **/
public interface JudgeStrategy {
    /**
     * 执行判题接口
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext);
}
