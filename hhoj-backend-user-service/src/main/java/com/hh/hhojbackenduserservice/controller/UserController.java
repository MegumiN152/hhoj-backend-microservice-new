package com.hh.hhojbackenduserservice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hh.hhojbackendcommon.annotation.AuthCheck;
import com.hh.hhojbackendcommon.common.BaseResponse;
import com.hh.hhojbackendcommon.common.DeleteRequest;
import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendcommon.common.ResultUtils;
import com.hh.hhojbackendcommon.constant.UserConstant;
import com.hh.hhojbackendcommon.utils.JwtUtils;
import com.hh.hhojbackendmodel.dto.questionsubmit.QuestionSubmitQueryDTO;
import com.hh.hhojbackendmodel.dto.user.*;
import com.hh.hhojbackendmodel.entity.Question;
import com.hh.hhojbackendmodel.entity.User;
import com.hh.hhojbackendmodel.vo.*;
import com.hh.hhojbackendserviceclient.service.QuestionFeignClient;
import com.hh.hhojbackenduserservice.config.MinioConfiguration;
import com.hh.hhojbackenduserservice.exception.BusinessException;
import com.hh.hhojbackenduserservice.exception.ThrowUtils;
import com.hh.hhojbackenduserservice.service.UserService;
import io.jsonwebtoken.Claims;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户接口
 *
 */
@RestController
@RequestMapping("/")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    private QuestionFeignClient questionFeignClient;

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioConfiguration minioConfiguration;

    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file, @RequestParam("userId") Long userId) {
        try {
            // 校验文件类型
            String contentType = file.getContentType();
            if (contentType != null && !contentType.startsWith("image/")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "只能上传图片文件");
            }

            // 校验文件大小（例如最大 2MB）
            long maxSize = 2 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 2MB");
            }

            // 生成文件名：userId_timestamp.extension
            String extension = getFileExtension(file.getOriginalFilename());
            String fileName = String.format("avatar/%s_%s%s", userId, System.currentTimeMillis(), ".jpg");

            // 上传到 MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfiguration.getBucket())  // MinIO bucket 名称
                            .object(fileName)       // 文件名
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
            // 获取文件访问URL
            String avatarUrl = String.format("%s/%s/%s",minioConfiguration.getEndpoint(),minioConfiguration.getBucket(),fileName);
            // 更新用户头像URL
            User user = userService.getById(userId);
            if (user != null) {
                // 删除旧头像文件（如果存在）
                String oldAvatarUrl = user.getUserAvatar();
                if (StringUtils.isNotBlank(oldAvatarUrl)) {
                    try {
                        String oldFileName = extractFilePathFromUrl(oldAvatarUrl);
                        minioClient.removeObject(
                                RemoveObjectArgs.builder()
                                        .bucket(minioConfiguration.getBucket())
                                        .object(oldFileName)
                                        .build()
                        );
                    } catch (Exception e) {
                        // 记录日志但不影响新文件上传
                        log.error("删除旧头像文件失败", e);
                    }
                }
                user.setUserAvatar(avatarUrl);
                userService.updateById(user);
            }
            return ResultUtils.success(avatarUrl);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件上传失败");
        }
    }
    // region 登录相关
    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        // 1. 从请求头获取 Token
        String token = request.getHeader("Authorization");
        if (StringUtils.isBlank(token) || !token.startsWith("Bearer ")) {
            return ResultUtils.success(null); // 未携带 Token，返回未登录
        }
        token = token.substring(7);

        try {
            // 2. 解析 Token 获取用户 ID
            Claims claims = JwtUtils.parseToken(token);
            Long userId = Long.parseLong(claims.get("userId",String.class));

            // 3. 查询用户信息
            User user = userService.getById(userId);
            if (user == null) {
                return ResultUtils.success(null);
            }
            return ResultUtils.success(userService.getLoginUserVO(user));
        } catch (Exception e) {
            log.info(e.getMessage());
            // Token 解析失败，返回未登录
            return ResultUtils.success(null);
        }
    }

    /**
     * 获取当前登录用户的提交记录
     *
     * @param request
     * @return
     */
    @GetMapping("/get/userquestion")
    public BaseResponse<UserQuestionVO> getLoginUserQuestion(HttpServletRequest request) {
        UserQuestionVO userQuestionVO = getUserQuestionVO(request, null);
        return ResultUtils.success(userQuestionVO);
    }
    private UserQuestionVO getUserQuestionVO(HttpServletRequest request,Long questionId) {
        User user = userService.getLoginUser(request);
        LoginUserVO loginUserVO = userService.getLoginUserVO(user);
        UserQuestionVO userQuestionVO = new UserQuestionVO();
        BeanUtils.copyProperties(loginUserVO, userQuestionVO);
        QuestionSubmitQueryDTO questionSubmitQueryDTO = new QuestionSubmitQueryDTO();
        questionSubmitQueryDTO.setUserId(user.getId());
        if (questionId!=null){
            questionSubmitQueryDTO.setQuestionId(questionId);
        }
        List<QuestionSubmitVO> userQuestionsList = questionFeignClient.list(questionSubmitQueryDTO).stream().map(QuestionSubmitVO::objToVo).peek(questionSubmitVO -> {
            Question question = questionFeignClient.getOne(questionSubmitVO.getQuestionId());
            QuestionVO questionVO=QuestionVO.objToVo(question);
            questionSubmitVO.setQuestionVO(questionVO);
        }).collect(Collectors.toList());
        userQuestionVO.setQuestionSubmitList(userQuestionsList);
        return userQuestionVO;
    }

    /**
     * 获取当前登录用户的提交记录
     *
     * @param request
     * @return
     */
    @GetMapping("/get/userquestion/{questionId}")
    public BaseResponse<UserQuestionVO> getLoginUserQuestionByQuestionId(@PathVariable("questionId") Long questionId, HttpServletRequest request) {
        UserQuestionVO userQuestionVO = getUserQuestionVO(request, questionId);
        return ResultUtils.success(userQuestionVO);
    }

    // endregion

    // region 增删改查

    /**
     * 创建用户
     *
     * @param userAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 校验参数
        Long result = userService.userAdd(userAddRequest);
        return ResultUtils.success(result);
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (Objects.equals(deleteRequest.getId(), loginUser.getId())){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"不能删除自己");
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest,
            HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userUpdate(userUpdateRequest);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取用户（仅管理员）
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id, HttpServletRequest request) {
        BaseResponse<User> response = getUserById(id, request);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 分页获取用户列表（仅管理员）
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest,
            HttpServletRequest request) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        return ResultUtils.success(userPage);
    }

    /**
     * 分页获取用户封装列表
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest,
            HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userService.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return ResultUtils.success(userVOPage);
    }

    // endregion

    /**
     * 更新个人信息
     *
     * @param userUpdateMyRequest
     * @param request
     * @return
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
            HttpServletRequest request) {
        if (userUpdateMyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        User user = new User();
        BeanUtils.copyProperties(userUpdateMyRequest, user);
        user.setId(loginUser.getId());
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
    /**
     * 从URL中提取文件路径
     * 例如：从 http://192.168.124.106:9000/hhoj/avatar/xxx.jpg 提取出 avatar/xxx.jpg
     */
    private String extractFilePathFromUrl(String url) {
        try {
            if (StringUtils.isBlank(url)) {
                return null;
            }
            // 查找最后一个斜杠之前的 "hhoj/" 位置
            int bucketIndex = url.lastIndexOf(minioConfiguration.getBucket() + "/");
            if (bucketIndex != -1) {
                // 返回 bucket 名称后的路径部分
                return url.substring(bucketIndex + minioConfiguration.getBucket().length() + 1);
            }
            return null;
        } catch (Exception e) {
            log.error("提取文件路径失败, url: {}", url, e);
            return null;
        }
    }
    private String getFileExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf(".")))
                .orElse("");
    }

}
