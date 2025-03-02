package com.hh.hhojbackendmodel.codeSandBox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author 黄昊
 * @version 1.0
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeRequest {
    /**
     *一组题目输入
     */
    private List<String> inputList;
    /**
     *题目要执行的代码
     */
    private String code;
    /**
     *题目语言
     */
    private String language;
}
