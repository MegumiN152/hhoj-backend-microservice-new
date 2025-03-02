package com.hh.hhojbackendmodel.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author 黄昊
 * @version 1.0
 **/
@Data
public class UserQuestionVO {
    /**
     * 用户 id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/ban
     */
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 最近的题目提交记录
     */
    private List<QuestionSubmitVO > questionSubmitList;

    private static final long serialVersionUID = 1L;

}
