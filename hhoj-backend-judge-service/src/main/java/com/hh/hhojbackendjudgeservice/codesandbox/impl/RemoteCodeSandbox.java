package com.hh.hhojbackendjudgeservice.codesandbox.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendjudgeservice.codesandbox.CodeSandbox;
import com.hh.hhojbackendjudgeservice.exception.BusinessException;
import com.hh.hhojbackendmodel.codeSandBox.ExecuteCodeRequest;
import com.hh.hhojbackendmodel.codeSandBox.ExecuteCodeResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * @author 黄昊
 * @version 1.0
 * 远程代码沙箱（实际调用接口的沙箱）
 **/
public class RemoteCodeSandbox implements CodeSandbox {
    private static final String AUTH_REQUEST_HEADER="auth";
    private static final String AUTH_REQUEST_SECRET="secretKey";
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("远程代码沙箱");
        String url ="http://192.168.0.198:8082/executeCode";
        String json= JSONUtil.toJsonStr(executeCodeRequest);
        String responseStr = HttpUtil.createPost(url)
                .body(json)
                .header(AUTH_REQUEST_HEADER,AUTH_REQUEST_SECRET)
                .execute()
                .body();
        if (StringUtils.isBlank(responseStr)){
            throw new BusinessException(ErrorCode.API_REQUEST_ERROR,"executeCode remoteSandbox error, message="+responseStr);
        }
        return JSONUtil.toBean(responseStr,ExecuteCodeResponse.class);
    }
}
