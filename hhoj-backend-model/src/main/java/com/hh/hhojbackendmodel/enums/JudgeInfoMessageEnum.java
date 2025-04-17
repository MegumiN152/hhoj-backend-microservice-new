package com.hh.hhojbackendmodel.enums;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 判题信息消息枚举
 *

 */
@Getter
public enum JudgeInfoMessageEnum {

    ACCEPTED("成功", "Accepted"),
    WRONG_ANSWER("答案错误", "Wrong Answer"),
    COMPILE_ERROR("Compile Error", "编译错误"),
    MEMORY_LIMIT_EXCEEDED("", "内存溢出"),
    TIME_LIMIT_EXCEEDED("Time Limit Exceeded", "超时"),
    PRESENTATION_ERROR("Presentation Error", "展示错误"),
    WAITING("Waiting", "等待中"),
    OUTPUT_LIMIT_EXCEEDED("Output Limit Exceeded", "输出溢出"),
    DANGEROUS_OPERATION("Dangerous Operation", "危险操作"),
    RUNTIME_ERROR("Runtime Error", "运行错误"),
    SYSTEM_ERROR("System Error", "系统错误"),
    LANGUAGE_ERROR("Language Error","不支持的语言"),
    ;

    private final String text;

    private final String value;

    private static final Map<String,JudgeInfoMessageEnum> VALUE_MAP=new HashMap<>();

    static {
        for (JudgeInfoMessageEnum judgeInfoMessageEnum:values()){
            VALUE_MAP.put(judgeInfoMessageEnum.value,judgeInfoMessageEnum);
        }
    }

    JudgeInfoMessageEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static JudgeInfoMessageEnum getEnumByValue(String value){
        if (StringUtils.isBlank(value)){
            return null;
        }
        return VALUE_MAP.get(value);
    }
}
