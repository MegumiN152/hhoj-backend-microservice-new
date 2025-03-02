package com.hh.hhojbackendmodel.codeSandBox;

import lombok.Data;

/**
 * 判题信息
 * @author 黄昊
 * @version 1.0
 **/
@Data
public class JudgeInfo {
    /**
     * 内存 kb
     */
    private Long memory;
    /**
     * 时间 ms
     */
    private Long time;
    /**
     * 程序执行信息
     */
    private String message;
}
