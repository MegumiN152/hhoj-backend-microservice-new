package com.hh.hhojbackendmodel.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 黄昊
 * @version 1.0
 **/
@Data
public class UserLeaderboardVO implements Serializable {
    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 解决题目数
     */
    private Long solvedCount;

    /**
     * 提交次数
     */
    private Long submitCount;

}
