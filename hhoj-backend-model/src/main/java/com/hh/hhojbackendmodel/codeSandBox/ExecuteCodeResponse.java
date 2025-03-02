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
public class ExecuteCodeResponse {
    /**
     *一组题目输出
     */
    private List<String> outputList;
    /**
     *代码执行信息
     */
    private String message;
    /**
     *代码执行状态
     */
    private Integer status ;
    /**
     *判题信息
     */
    private JudgeInfo judgeInfo;
}
