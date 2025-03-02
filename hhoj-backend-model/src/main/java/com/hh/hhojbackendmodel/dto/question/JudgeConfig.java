package com.hh.hhojbackendmodel.dto.question;

import lombok.Data;

/**
 * 题目配置
 * @author 黄昊
 * @version 1.0
 **/
@Data
public class JudgeConfig {
    /**
     * 时间限制 ms
     */
    private Long timeLimit;
    /**
     * 内存限制 kb
     */
    private Long memoryLimit;
    /**
     * 堆栈限制 kb
     */
    private Long stackLimit;
}
