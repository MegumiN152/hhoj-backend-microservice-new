package com.hh.hhojbackendmodel.dto.question;

import lombok.Data;

/**
 * 题目用例
 * @author 黄昊
 * @version 1.0
 **/
@Data
public class JudgeCase {
    /**
     * 输入用例
     */
    private String input;
    /**
     * 输出用例
     */
    private String output;
}
