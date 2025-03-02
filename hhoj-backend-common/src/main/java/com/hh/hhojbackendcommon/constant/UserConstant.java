package com.hh.hhojbackendcommon.constant;

/**
 * 用户常量
 *
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 被封号
     */
    String BAN_ROLE = "ban";
    /**
     * 用户默认头像
     */
    String DEFAULT_AVATAR="https://img2.baidu.com/it/u=2673297632,3717596799&fm=253&fmt=auto&app=138&f=JPEG?w=342&h=342";
    /**
     * 用户默认简介
     */
    String DEFAULT_INTRODUCE="这个人很懒，什么都没有留下";
    // endregion
}
