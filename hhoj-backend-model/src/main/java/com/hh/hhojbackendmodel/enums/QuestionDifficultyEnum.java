package com.hh.hhojbackendmodel.enums;

/**
 * @author 黄昊
 * @version 1.0
 **/
public enum QuestionDifficultyEnum {

    SIMPLE("简单", 0),
    MEDIUM("中等", 1),
    DIFFICULT("困难", 2),
            ;

    private final String text;

    private final Integer value;

     QuestionDifficultyEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public Integer getValue() {
        return value;
    }
    public static QuestionDifficultyEnum getEnumByValue(Integer value) {
        for (QuestionDifficultyEnum questionDifficultyEnum : QuestionDifficultyEnum.values()) {
            if (questionDifficultyEnum.getValue().equals(value)) {
                return questionDifficultyEnum;
            }
        }
        return null;
    }
}
