package com.hh.hhojbackendquestionservice.manager;


import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendquestionservice.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author 黄昊
 * @version 1.0
 * 专门提供redis基础服务的(提供了通用的能力)
 **/
@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 限流操作
     */
    public void doRateLimit(String key) {
        //创建一个限流器，每五秒钟限制两次
        RRateLimiter rRateLimiter = redissonClient.getRateLimiter(key);
        rRateLimiter.trySetRate(RateType.OVERALL, 2, 5, RateIntervalUnit.SECONDS);

        //每当一个操作来了后，请求一个令牌
        boolean b = rRateLimiter.tryAcquire(1);
        if (!b) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
    /**
     * 限流操作
     */
    public void doRateLimit_genQuestion(String key) {
        //创建一个限流器，每五秒钟限制两次
        RRateLimiter rRateLimiter = redissonClient.getRateLimiter(key);
        rRateLimiter.trySetRate(RateType.OVERALL, 1, 1, RateIntervalUnit.MINUTES);

        //每当一个操作来了后，请求一个令牌
        boolean b = rRateLimiter.tryAcquire(1);
        if (!b) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
