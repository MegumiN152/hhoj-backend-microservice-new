package com.hh.hhojbackendmodel.dto.question;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 题目生成请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionGenerateRequest implements Serializable {

    /**
     * 题目标题（可能是草稿或初步想法）
     */
    private String title;

    /**
     * 题目标签
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}
