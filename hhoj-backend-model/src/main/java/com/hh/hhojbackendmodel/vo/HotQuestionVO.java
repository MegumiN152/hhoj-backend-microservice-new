package com.hh.hhojbackendmodel.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author 黄昊
 * @version 1.0
 **/
@Data
public class HotQuestionVO implements Serializable {
    private Long id;
    private String title;
    private List<String> tags;
    private Integer difficulty;
}
