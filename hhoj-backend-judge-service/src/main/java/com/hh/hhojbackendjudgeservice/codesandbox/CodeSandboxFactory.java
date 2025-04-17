package com.hh.hhojbackendjudgeservice.codesandbox;


import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendjudgeservice.codesandbox.impl.ExampleCodeSandbox;
import com.hh.hhojbackendjudgeservice.codesandbox.impl.RemoteCodeSandbox;
import com.hh.hhojbackendjudgeservice.codesandbox.impl.ThirdPartyCodeSandbox;
import com.hh.hhojbackendserviceclient.exception.BusinessException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
/**
 * @author 黄昊
 * @version 1.0
 * 代码沙箱创建工厂
 **/
@Component
public class CodeSandboxFactory {
    @Resource
    private Map<String,CodeSandbox> stringCodeSandboxMap;
    /**
     * 创建代码沙箱实例
     * @param type 代码沙箱类型
     * @return
     */
    public  CodeSandbox newInstance(String type) {
//        switch (type){
//            case "example":
//                return new ExampleCodeSandbox();
//            case "remote":
//                return new RemoteCodeSandbox();
//            case "thirdParty":
//                return new ThirdPartyCodeSandbox();
//        }
        CodeSandbox codeSandbox = stringCodeSandboxMap.get(type.toLowerCase());
        if (codeSandbox==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"无效的代码沙箱类型："+type);
        }
        return codeSandbox;
    }
}
