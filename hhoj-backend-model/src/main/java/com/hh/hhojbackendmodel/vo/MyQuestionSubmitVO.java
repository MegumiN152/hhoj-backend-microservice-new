package com.hh.hhojbackendmodel.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author 黄昊
 * @version 1.0
 **/
@Data
public class MyQuestionSubmitVO implements Serializable {
    /**
     * 题目id
     */
    private Long id;
    /**
     * 题目标题
     */
    private String title;
    /**
     * 提交状态
     */
    private Integer status;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    /**
     * 语言
     */
    private String language;

    private static final long serialVersionUID = 1L;
}
