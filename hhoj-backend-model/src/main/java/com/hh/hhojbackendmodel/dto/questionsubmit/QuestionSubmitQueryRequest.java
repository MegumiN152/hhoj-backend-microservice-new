package com.hh.hhojbackendmodel.dto.questionsubmit;


import com.hh.hhojbackendcommon.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 题目提交查询请求
 *

 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionSubmitQueryRequest extends PageRequest implements Serializable {


    /**
     * 编程语言
     */
    private String language;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 题目 id
     */
    private Long questionId;
    /**
     * 用户id
     */
    private Long userId;



    private static final long serialVersionUID = 1L;
}