package com.hh.hhojbackendjudgeservice.codesandbox.impl;


import com.hh.hhojbackendjudgeservice.codesandbox.CodeSandbox;
import com.hh.hhojbackendmodel.codeSandBox.ExecuteCodeRequest;
import com.hh.hhojbackendmodel.codeSandBox.ExecuteCodeResponse;

/**
 * @author 黄昊
 * @version 1.0
 * 第三方代码沙箱（调用非我们自己开发的沙箱）
 **/
public class ThirdPartyCodeSandbox implements CodeSandbox {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("第三方代码沙箱");
        return null;
    }
}
