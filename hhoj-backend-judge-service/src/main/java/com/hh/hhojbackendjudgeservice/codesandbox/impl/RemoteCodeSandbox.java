package com.hh.hhojbackendjudgeservice.codesandbox.impl;

import cn.hutool.http.HttpException;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendjudgeservice.codesandbox.CodeSandbox;
import com.hh.hhojbackendjudgeservice.exception.BusinessException;
import com.hh.hhojbackendmodel.codeSandBox.ExecuteCodeRequest;
import com.hh.hhojbackendmodel.codeSandBox.ExecuteCodeResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author 黄昊
 * @version 1.0
 * 远程代码沙箱（实际调用接口的沙箱）
 **/
@Service("remote")
public class RemoteCodeSandbox implements CodeSandbox {
    @Value("${codesandbox.remote.url}")
    private String remoteUrl;

    @Value("${codesandbox.remote.auth-header}")
    private String auth_header;

    @Value("${codesandbox.remote.auth-secret}")
    private String auth_secret;
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("远程代码沙箱");
        String json= JSONUtil.toJsonStr(executeCodeRequest);
        String responseStr = null;
        try {
            responseStr = HttpUtil.createPost(remoteUrl)
                    .body(json)
                    .header(auth_header,auth_secret)
                    .execute()
                    .body();
        } catch (HttpException e) {
            throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "远程沙箱调用失败: " + e.getMessage());
        }
        if (StringUtils.isBlank(responseStr)){
            throw new BusinessException(ErrorCode.API_REQUEST_ERROR,"executeCode remoteSandbox error, message="+responseStr);
        }
        return JSONUtil.toBean(responseStr,ExecuteCodeResponse.class);
    }
}
