package com.hh.hhojbackendmodel.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户创建请求
 *

 */
@Data
public class UserAddRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户角色: user, admin
     */
    private String userRole;
    /**
     * 用户密码
     */
    private String userPassword;
    /**
     * 确认密码
     */
    private String checkPassword;
    /**
     * 用户简介
     */
    private String userProfile;

    private static final long serialVersionUID = 1L;
}