package com.hh.hhojbackendmodel.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiQuestionVO implements Serializable {
    private String result;
    private String code;
    private Long aiQuestionId;
}