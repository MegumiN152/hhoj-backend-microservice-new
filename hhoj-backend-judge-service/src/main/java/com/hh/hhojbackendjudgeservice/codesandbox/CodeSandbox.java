package com.hh.hhojbackendjudgeservice.codesandbox;


import com.hh.hhojbackendmodel.codeSandBox.ExecuteCodeRequest;
import com.hh.hhojbackendmodel.codeSandBox.ExecuteCodeResponse;

/**
 * @author 黄昊
 * @version 1.0
 **/
public interface CodeSandbox {
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
