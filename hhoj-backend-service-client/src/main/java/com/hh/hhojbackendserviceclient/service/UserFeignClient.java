package com.hh.hhojbackendserviceclient.service;

import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendmodel.entity.User;
import com.hh.hhojbackendmodel.enums.UserRoleEnum;
import com.hh.hhojbackendmodel.vo.UserVO;
import com.hh.hhojbackendserviceclient.exception.BusinessException;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;

import static com.hh.hhojbackendcommon.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务
 */
@FeignClient(name = "hhoj-backend-user-service", path = "/api/user/inner")
public interface UserFeignClient {


    /**
     * 根据id获取用户
     *
     * @param userId
     * @return
     */
    @GetMapping("/get/id")
    User getById(@RequestParam("userId") long userId);

    /**
     * 根据id获取用户列表
     *
     * @param idList
     * @return
     */
    @GetMapping("/get/ids")
    List<User> listByIds(@RequestParam("idList") Collection<Long> idList);

    @PostMapping("/update/id")
    boolean updateById(@RequestBody User user);
    @RequestMapping("/logout")
    public boolean logout(@RequestParam("token") String token);
    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    default User getLoginUser(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr==null && userIdStr.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        long userId = Long.parseLong(userIdStr);
       User currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if(UserRoleEnum.BAN.getValue().equals(currentUser.getUserRole())){
            //强制下线
            String token = request.getHeader("Authorization");
            this.logout(token);
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"该账号已被封禁");
        }
        return currentUser;
    }

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    default boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    default UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }
}
