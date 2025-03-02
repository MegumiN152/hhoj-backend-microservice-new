package com.hh.hhojbackendjudgeservice.codesandbox;


import com.hh.hhojbackendjudgeservice.codesandbox.impl.ExampleCodeSandbox;
import com.hh.hhojbackendjudgeservice.codesandbox.impl.RemoteCodeSandbox;
import com.hh.hhojbackendjudgeservice.codesandbox.impl.ThirdPartyCodeSandbox;

/**
 * @author 黄昊
 * @version 1.0
 * 代码沙箱创建工厂
 **/
public class CodeSandboxFactory {
    /**
     * 创建代码沙箱实例
     * @param type 代码沙箱类型
     * @return
     */
    public static CodeSandbox newInstance(String type) {
        switch (type){
            case "example":
                return new ExampleCodeSandbox();
            case "remote":
                return new RemoteCodeSandbox();
            case "thirdParty":
                return new ThirdPartyCodeSandbox();
        }
        return null;
    }
}
