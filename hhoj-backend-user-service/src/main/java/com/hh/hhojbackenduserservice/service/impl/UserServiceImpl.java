package com.hh.hhojbackenduserservice.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendcommon.constant.CommonConstant;
import com.hh.hhojbackendcommon.constant.UserConstant;
import com.hh.hhojbackendcommon.utils.JwtUtils;
import com.hh.hhojbackendcommon.utils.SqlUtils;
import com.hh.hhojbackendmodel.dto.user.UserAddRequest;
import com.hh.hhojbackendmodel.dto.user.UserQueryRequest;
import com.hh.hhojbackendmodel.dto.user.UserUpdateRequest;
import com.hh.hhojbackendmodel.entity.User;
import com.hh.hhojbackendmodel.enums.UserRoleEnum;
import com.hh.hhojbackendmodel.vo.LoginUserVO;
import com.hh.hhojbackendmodel.vo.UserVO;
import com.hh.hhojbackenduserservice.exception.BusinessException;
import com.hh.hhojbackenduserservice.mapper.UserMapper;
import com.hh.hhojbackenduserservice.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hh.hhojbackendcommon.constant.UserConstant.USER_LOGIN_STATE;


/**
 * 用户服务实现
 *

 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 盐值，混淆密码
     */
    public static final String SALT = "yupi";

    @Resource
    private RedisTemplate<String,String> redisTemplate;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            //设置默认用户名称
            user.setUserName(userAccount);
            //设置默认头像
            user.setUserAvatar(UserConstant.DEFAULT_AVATAR);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public long userAdd(UserAddRequest userAddRequest) {
        String userName = userAddRequest.getUserName();
        String userAccount = userAddRequest.getUserAccount();
        String userAvatar = userAddRequest.getUserAvatar();
        String userRole = userAddRequest.getUserRole();
        String userPassword = userAddRequest.getUserPassword();
        String checkPassword = userAddRequest.getCheckPassword();
        String userProfile = userAddRequest.getUserProfile();
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (StringUtils.isBlank(userName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名为空");
        }
        if (!(userRole.equals(UserConstant.ADMIN_ROLE) || userRole.equals(UserConstant.DEFAULT_ROLE))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户角色错误");
        }

        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            //设置用户名称
            user.setUserName(userName);
            //设置用户角色
            user.setUserRole(userRole);
            if (!StringUtils.isBlank(userProfile)&&userProfile.length()<30){
                user.setUserProfile(userProfile);
            } else if (StringUtils.isBlank(userProfile)) {
                //设置个人简介
                user.setUserProfile(UserConstant.DEFAULT_INTRODUCE);
            }
            //设置用户头像
            if (StringUtils.isNotBlank(userAvatar)) {
                user.setUserAvatar(userAvatar);
            } else {
                user.setUserAvatar(UserConstant.DEFAULT_AVATAR);
            }
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加用户失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public boolean userUpdate(UserUpdateRequest userUpdateRequest) {
        Long id = userUpdateRequest.getId();
        String userName = userUpdateRequest.getUserName();
        String userAvatar = userUpdateRequest.getUserAvatar();
        String userProfile = userUpdateRequest.getUserProfile();
        String userRole = userUpdateRequest.getUserRole();
        String userPassword = userUpdateRequest.getUserPassword();

        // 3. 插入数据
        User user = new User();
        // 1. 校验
        if (id == null || id <= 0 || StringUtils.isAnyBlank(userRole, userName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id为空");
        }else {
            user.setId(id);
            user.setUserName(userName);
            user.setUserRole(userRole);
        }
        if (!StringUtils.isBlank(userPassword) && userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }else if (!StringUtils.isBlank(userPassword)){
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            user.setUserPassword(encryptPassword);
        }
        if (!StringUtils.isBlank(userProfile)&&userProfile.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "个人简介过长");
        }else if(!StringUtils.isBlank(userProfile)){
            user.setUserProfile(userProfile);
        }
        if (UserRoleEnum.getEnumByValue(userRole) ==null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户角色错误");
        }
        return this.updateById(user);
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        if (user.getUserRole().equals(UserRoleEnum.BAN.getValue())){
            log.info("该账号已被封禁："+user.getId());
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"该账号已被封禁");
        }
        LoginUserVO loginUserVO = this.getLoginUserVO(user);
        // 3. 记录用户的登录态
        String token = JwtUtils.generateToken(loginUserVO.getId(),loginUserVO.getUserRole());
        loginUserVO.setToken(token);
        return loginUserVO;
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr==null || userIdStr.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        long userId = Long.parseLong(userIdStr);
        User currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if(UserRoleEnum.BAN.getValue().equals(currentUser.getUserRole())){
            //强制下线
            boolean b = this.userLogout(request);
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"该账号已被封禁");
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr==null || userIdStr.isEmpty()) {
            return null;
        }
        long userId = Long.parseLong(userIdStr);
        User currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        User loginUser = getLoginUser(request);
        String userRoleStr = request.getHeader("X-User-Role");
        if (userRoleStr==null || userRoleStr.isEmpty()) {
           throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return isAdmin(loginUser);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 1. 从请求头中获取 Token
        String token = request.getHeader("Authorization");
        if (StringUtils.isBlank(token) || !token.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未提供 Token");
        }
        token = token.substring(7); // 去除 "Bearer " 前缀

        // 2. 解析 Token 获取过期时间（可选）
        Claims claims = JwtUtils.parseToken(token);
        Date expiration = claims.getExpiration();

        // 3. 将 Token 加入黑名单（Redis）
        long ttl = expiration.getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            // 使用 Redis 存储黑名单，Key 格式：jwt:blacklist:<token>
            redisTemplate.opsForValue().set(
                    "jwt:blacklist:" + token,
                    "logged_out",
                    ttl,
                    TimeUnit.MILLISECONDS
            );
        }

        return true;
    }
    /**
     * 用户注销
     *
     */
    @Override
    public boolean userLogoutBytoken(String token) {
        if (StringUtils.isBlank(token) || !token.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未提供 Token");
        }
        token = token.substring(7); // 去除 "Bearer " 前缀

        // 2. 解析 Token 获取过期时间（可选）
        Claims claims = JwtUtils.parseToken(token);
        Date expiration = claims.getExpiration();

        // 3. 将 Token 加入黑名单（Redis）
        long ttl = expiration.getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            // 使用 Redis 存储黑名单，Key 格式：jwt:blacklist:<token>
            redisTemplate.opsForValue().set(
                    "jwt:blacklist:" + token,
                    "logged_out",
                    ttl,
                    TimeUnit.MILLISECONDS
            );
        }

        return true;
    }


    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String userAccount = userQueryRequest.getUserAccount();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(unionId), "unionId", unionId);
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mpOpenId", mpOpenId);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StringUtils.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
}
