package com.hh.hhojbackendjudgeservice.codesandbox;


import com.hh.hhojbackendmodel.codeSandBox.ExecuteCodeRequest;
import com.hh.hhojbackendmodel.codeSandBox.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 黄昊
 * @version 1.0
 **/
@Slf4j
public class CodeSandboxProxy implements CodeSandbox{

    private  CodeSandbox codeSandbox;

    public CodeSandboxProxy(CodeSandbox codeSandbox) {
        this.codeSandbox = codeSandbox;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        log.info("代理类调用开始");
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        log.info("代理类调用结束");
        return executeCodeResponse;
    }
}
