package com.hh.hhojbackenduserservice.controller.inner;

import com.hh.hhojbackendmodel.entity.User;
import com.hh.hhojbackendserviceclient.service.UserFeignClient;
import com.hh.hhojbackenduserservice.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;

/**
 * @author 黄昊
 * @version 1.0
 **/
@RestController
@RequestMapping("/inner")
public class UserInnerController implements UserFeignClient {
    @Resource
    private UserService userService;

    /**
     * 根据id获取用户
     *
     * @param userId
     * @return
     */
    @Override
    @GetMapping("/get/id")
    public User getById(@RequestParam("userId") long userId) {
        return userService.getById(userId);
    }

    /**
     * 根据id获取用户列表
     *
     * @param idList
     * @return
     */
    @Override
    @GetMapping("/get/ids")
    public List<User> listByIds(@RequestParam("idList") Collection<Long> idList) {
        return userService.listByIds(idList);
    }
    @Override
    @PostMapping("/update/id")
    public boolean updateById(@RequestBody User user){
        return userService.updateById(user);
    }

    @Override
    @RequestMapping("/logout")
    public boolean logout(@RequestParam("token") String token){
        return userService.userLogoutBytoken(token);
    }
}
