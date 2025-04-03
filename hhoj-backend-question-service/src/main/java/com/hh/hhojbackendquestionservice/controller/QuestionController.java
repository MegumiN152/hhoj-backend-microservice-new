package com.hh.hhojbackendquestionservice.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hh.hhojbackendcommon.annotation.AuthCheck;
import com.hh.hhojbackendcommon.common.BaseResponse;
import com.hh.hhojbackendcommon.common.DeleteRequest;
import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendcommon.common.ResultUtils;
import com.hh.hhojbackendcommon.constant.RedisConstant;
import com.hh.hhojbackendcommon.constant.UserConstant;
import com.hh.hhojbackendmodel.dto.question.*;
import com.hh.hhojbackendmodel.dto.questionsubmit.QuestionSubmitAddRequest;
import com.hh.hhojbackendmodel.dto.questionsubmit.QuestionSubmitQueryDTO;
import com.hh.hhojbackendmodel.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.hh.hhojbackendmodel.entity.Question;
import com.hh.hhojbackendmodel.entity.QuestionSubmit;
import com.hh.hhojbackendmodel.entity.User;
import com.hh.hhojbackendmodel.enums.QuestionDifficultyEnum;
import com.hh.hhojbackendmodel.enums.QuestionsSubmitLanguageEnum;
import com.hh.hhojbackendmodel.vo.*;
import com.hh.hhojbackendquestionservice.exception.BusinessException;
import com.hh.hhojbackendquestionservice.exception.ThrowUtils;
import com.hh.hhojbackendquestionservice.job.CacheClearTask;
import com.hh.hhojbackendquestionservice.manager.AiManager;
import com.hh.hhojbackendquestionservice.manager.RedisLimiterManager;
import com.hh.hhojbackendquestionservice.service.QuestionService;
import com.hh.hhojbackendquestionservice.service.QuestionSubmitService;
import com.hh.hhojbackendserviceclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hh.hhojbackendcommon.constant.RedisConstant.CACHE_KEY_PREFIX;

/**
 * 题目接口
 */
@RestController
@RequestMapping("/")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private AiManager aiManager;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private CacheClearTask cacheClearTask;

    // region 增删改查

    /**
     * 创建
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        if (questionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<String> tags = questionAddRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        Integer difficulty = questionAddRequest.getDifficulty();
        if (QuestionDifficultyEnum.getEnumByValue(difficulty) != null) {
            question.setDifficulty(difficulty);
        }
        List<JudgeCase> judgeCase = questionAddRequest.getJudgeCase();
        JudgeConfig judgeConfig = questionAddRequest.getJudgeConfig();
        if (judgeCase != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCase));
        }
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        questionService.validQuestion(question, true);
        User loginUser = userFeignClient.getLoginUser(request);
        question.setUserId(loginUser.getId());
        question.setFavourNum(0);
        question.setThumbNum(0);
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userFeignClient.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userFeignClient.isAdmin(user)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = questionService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        List<String> tags = questionUpdateRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        Integer difficulty = questionUpdateRequest.getDifficulty();
        if (QuestionDifficultyEnum.getEnumByValue(difficulty) != null) {
            question.setDifficulty(difficulty);
        }
        List<JudgeCase> judgeCase = questionUpdateRequest.getJudgeCase();
        JudgeConfig judgeConfig = questionUpdateRequest.getJudgeConfig();
        if (judgeCase != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCase));
        }
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        // 参数校验
        questionService.validQuestion(question, false);
        long id = questionUpdateRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = questionService.updateById(question);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request);
        return ResultUtils.success(questionService.getQuestionVO(question, loginUser));
    }

    /**
     * 根据 id 获取
     * 管理员专属
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Question> getQuestionById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(question);
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        if (questionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userFeignClient.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 编辑（用户）
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);
        List<String> tags = questionEditRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        Integer difficulty = questionEditRequest.getDifficulty();
        if (QuestionDifficultyEnum.getEnumByValue(difficulty) != null) {
            question.setDifficulty(difficulty);
        }
        List<JudgeCase> judgeCase = questionEditRequest.getJudgeCase();
        JudgeConfig judgeConfig = questionEditRequest.getJudgeConfig();
        if (judgeCase != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCase));
        }
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        // 参数校验
        questionService.validQuestion(question, false);
        User loginUser = userFeignClient.getLoginUser(request);
        long id = questionEditRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !userFeignClient.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = questionService.updateById(question);
        return ResultUtils.success(result);
    }
//    /**
//     *  通过questionId返回对应的答案
//     */
//    @GetMapping("/answer/{questionId}")
//    public BaseResponse<String> getAnswerByQuestionId(@PathVariable("questionId") Long questionId){
//        if (questionId == null || questionId <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR,"questionId不能为空");
//        }
//        Question question = questionService.getById(questionId);
//        if (question == null) {
//            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"题目不存在");
//        }
//        return ResultUtils.success(question.getAnswer());
//    }

    /**
     * 提交代码
     *
     * @param questionSubmitAddRequest
     * @param request
     * @return resultNum
     */
    @PostMapping("/question_submit/do")
    public BaseResponse<Long> doQuestionSubmit(@RequestBody QuestionSubmitAddRequest questionSubmitAddRequest,
                                               HttpServletRequest request) {
        if (questionSubmitAddRequest == null || questionSubmitAddRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能提交代码
        final User loginUser = userFeignClient.getLoginUser(request);
        //限流判断，每个用户加题目一个限流器 1分钟5次
        redisLimiterManager.doRateLimit(loginUser.getId() + "_question_submit");
        long questionSubmitId = questionSubmitService.doQuestionSubmit(questionSubmitAddRequest, loginUser);
        //更新数据库然后删除缓存
        log.info("删除缓存");
        clearUserCache(loginUser.getId());
        clearLeaderboardCache();
        clearHotQuestionsCache();
        return ResultUtils.success(questionSubmitId);
    }

//    @GetMapping(value = "/ai-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<String> aiQuestionStream(
//            @RequestParam("questionId") Long questionId,
//            @RequestParam("language") String language,
//            HttpServletRequest request) {
//
//        // 参数校验和用户登录验证
//        if (questionId == null || questionId <= 0) {
//            return Flux.error(new BusinessException(ErrorCode.PARAMS_ERROR));
//        }
////        User loginUser = userFeignClient.getLoginUser(request);
////        if (loginUser == null) {
////            return Flux.error(new BusinessException(ErrorCode.NOT_LOGIN_ERROR));
////        }
//
//        // 获取题目信息
//        Question question = questionService.getById(questionId);
//        if (question == null) {
//            return Flux.error(new BusinessException(ErrorCode.NOT_FOUND_ERROR));
//        }
//        // 直接获取AI原始流
//        return aiManager.getGenResultStream(
//                question.getTitle(),
//                question.getContent(),
//                language
//        );
//    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    @PostMapping("/question_submit/list/page")
    public BaseResponse<Page<QuestionSubmitVO>> listQuestionSubmitByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest, HttpServletRequest request) {
        long current = questionSubmitQueryRequest.getCurrent();
        long size = questionSubmitQueryRequest.getPageSize();
        User loginUser = userFeignClient.getLoginUser(request);
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(current, size),
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVOPage(questionSubmitPage, loginUser));
    }

    /**
     * 分页获取题目提交列表（用户自己）
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    @PostMapping("/question_submit/list/page/my")
    public BaseResponse<Page<MyQuestionSubmitVO>> listQuestionMySubmitByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest, HttpServletRequest request) {
        long current = questionSubmitQueryRequest.getCurrent();
        long size = questionSubmitQueryRequest.getPageSize();
        User loginUser = userFeignClient.getLoginUser(request);
        questionSubmitQueryRequest.setUserId(loginUser.getId());
        // 尝试从缓存获取
        String cacheKey = RedisConstant.USER_SUBMITS + loginUser.getId();
        Page<MyQuestionSubmitVO> cachedSubmits = (Page<MyQuestionSubmitVO>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedSubmits != null) {
            return ResultUtils.success(cachedSubmits);
        }
        // 缓存未命中，重新计算
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(current, size),
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        Page<MyQuestionSubmitVO> result = questionSubmitService.getMyQuestionSubmitVOPage(questionSubmitPage, loginUser);
        if (result.getRecords().isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES); // 空结果缓存5分钟
        } else {
            redisTemplate.opsForValue().set(cacheKey, result, 1, TimeUnit.HOURS); // 正常结果缓存1小时
        }
        return ResultUtils.success(result);
    }

    /**
     * ai答题
     */
    @PostMapping("/question/ai")
    public BaseResponse<AiQuestionVO> aiQuestion(@RequestBody QuestionSubmitQueryDTO questionSubmitQueryDTO, HttpServletRequest request) {
        if (questionSubmitQueryDTO == null || questionSubmitQueryDTO.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //如果没设编程语言，则默认设为java
        if (questionSubmitQueryDTO.getLanguage() == null) {
            questionSubmitQueryDTO.setLanguage(QuestionsSubmitLanguageEnum.JAVA.getValue());
        }
        User loginUser = userFeignClient.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Question question = questionService.getById(questionSubmitQueryDTO.getQuestionId());
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        //定义缓存键
        String cacheKey = CACHE_KEY_PREFIX + question.getId() + questionSubmitQueryDTO.getLanguage();
        // 尝试从Redis中获取缓存
        AiQuestionVO cachedAiQuestionVO = (AiQuestionVO) redisTemplate.opsForValue().get(cacheKey);
        if (cachedAiQuestionVO != null) {
            // 如果缓存存在，直接返回
            return ResultUtils.success(cachedAiQuestionVO);
        } else {
            // 如果缓存不存在，调用AI方法获取结果
            AiQuestionVO aiQuestionVO = aiManager.getGenResultByDeepSeek(question.getTitle(), question.getContent(), questionSubmitQueryDTO.getLanguage(), question.getId());

            // 将结果存入Redis并设置过期时间为一天
            redisTemplate.opsForValue().set(cacheKey, aiQuestionVO, 24, TimeUnit.HOURS);

            return ResultUtils.success(aiQuestionVO);
        }

    }

    /**
     * 统计个人数据
     *
     * @param request
     * @return
     */
    @GetMapping("/status")
    public BaseResponse<UserStatsVO> getUserStats(HttpServletRequest request) {
        User loginUser = userFeignClient.getLoginUser(request);
        //先从缓存获取
        String cacheKey = RedisConstant.USER_STATS + loginUser.getId();
        UserStatsVO cachedUserStatsVO = (UserStatsVO) redisTemplate.opsForValue().get(cacheKey);
        if (cachedUserStatsVO!=null){
            return ResultUtils.success(cachedUserStatsVO);
        }
        UserStatsVO userStatsVO = questionSubmitService.getUserStats(loginUser.getId());
        //设置缓存，过期时间1小时
        redisTemplate.opsForValue().set(cacheKey, userStatsVO, 1, TimeUnit.HOURS);
        return ResultUtils.success(userStatsVO);
    }

    @GetMapping("/leaderboard")
    public BaseResponse<List<UserLeaderboardVO>> getLeaderboard() {
        // 调用questionSubmitService的getLeaderboard方法获取排行榜
        // 尝试从缓存获取
        String cacheKey = RedisConstant.LEADERBOARD;
        List<UserLeaderboardVO> cachedLeaderboard = (List<UserLeaderboardVO>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedLeaderboard != null) {
            return ResultUtils.success(cachedLeaderboard);
        }
        List<UserLeaderboardVO> leaderboardVOS = questionSubmitService.getLeaderboard();
        // 设置缓存，过期时间1小时
        redisTemplate.opsForValue().set(cacheKey, leaderboardVOS, 1, TimeUnit.HOURS);
        return ResultUtils.success(leaderboardVOS);
    }

    /**
     * 返回近日热题列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/hot/list")
    public BaseResponse<Page<HotQuestionVO>> getHotQuestionSubmitList(@RequestBody QuestionQueryRequest questionQueryRequest, HttpServletRequest request) {
        User loginUser = userFeignClient.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        // 尝试从缓存获取
        String cacheKey = RedisConstant.HOT_QUESTIONS;
        Page<HotQuestionVO> cachedHotQuestionVO = (Page<HotQuestionVO>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedHotQuestionVO != null) {
            return ResultUtils.success(cachedHotQuestionVO);
        }
        Page<HotQuestionVO> questionVOPage = questionService.listHotQuestions(questionQueryRequest);
        // 设置缓存，过期时间1小时
        redisTemplate.opsForValue().set(cacheKey, questionVOPage, 1, TimeUnit.HOURS);
        return ResultUtils.success(questionVOPage);
    }

    /**
     * 清除用户相关的缓存
     */
    private void clearUserCache(Long userId) {
        String[] keys = new String[]{
                RedisConstant.USER_STATS + userId,
                RedisConstant.USER_SUBMITS + userId
        };
        // 使用延迟双删策略，延迟500ms
        cacheClearTask.delayedDoubleDelete(keys, 500);
    }

    /**
     * 清除排行榜缓存
     */
    private void clearLeaderboardCache() {
        String[] keys = new String[]{RedisConstant.LEADERBOARD};
        // 使用延迟双删策略，延迟500ms
        cacheClearTask.delayedDoubleDelete(keys, 500);
    }

    /**
     * 清除热门题目缓存
     */
    private void clearHotQuestionsCache() {
        String[] keys = new String[]{RedisConstant.HOT_QUESTIONS};
        // 使用延迟双删策略，延迟500ms
        cacheClearTask.delayedDoubleDelete(keys, 500);
    }
    /**
     * AI 生成编程题目
     *
     * @param questionGenerateRequest 题目生成请求
     * @param request HTTP请求
     * @return 生成的题目信息
     */
    @PostMapping("/generate")
    public BaseResponse<AiGeneratedQuestionVO> generateQuestion(@RequestBody QuestionGenerateRequest questionGenerateRequest, HttpServletRequest request) {
        // 参数校验
        if (questionGenerateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String title = questionGenerateRequest.getTitle();
        List<String> tags = questionGenerateRequest.getTags();

        // 标题和标签至少有一个不为空
        if ((title == null || title.isEmpty()) && (tags == null || tags.isEmpty())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目标题和标签至少提供一项");
        }

        // 登录且是管理员才能使用AI生成功能
        User loginUser = userFeignClient.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole()), ErrorCode.NO_AUTH_ERROR);

        // 调用AI生成题目
        AiGeneratedQuestionVO generatedQuestion = aiManager.generateProgrammingQuestion(title, tags);
        generatedQuestion.setSampleCode("```java "+generatedQuestion.getSampleCode());
        return ResultUtils.success(generatedQuestion);
    }
}
