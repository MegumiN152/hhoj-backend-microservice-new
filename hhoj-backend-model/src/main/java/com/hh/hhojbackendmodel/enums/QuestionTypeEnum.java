package com.hh.hhojbackendmodel.enums;

public enum QuestionTypeEnum {
    DeFAULT("普通", 0),
    AI("AI生成", 1),
    ;
    private final String text;

    private final Integer value;
    public String getText() {
        return text;
    }

    public Integer getValue() {
        return value;
    }
    QuestionTypeEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

}
