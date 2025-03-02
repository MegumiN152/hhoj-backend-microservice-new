package com.hh.hhojbackendcommentservice.manager;


import cn.hutool.extra.mail.MailUtil;
import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendserviceclient.exception.BusinessException;
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
     * 评论限流操作
     */
    public void doRateLimit(String key) {
        //创建一个限流器，每5秒钟2次
        RRateLimiter rRateLimiter = redissonClient.getRateLimiter(key);
        rRateLimiter.trySetRate(RateType.OVERALL, 2, 5, RateIntervalUnit.SECONDS);

        //每当一个操作来了后，请求一个令牌
        boolean b = rRateLimiter.tryAcquire(1);
        if (!b) {
            MailUtil.send("3105755134@qq.com", "评论限流告警", "这傻逼用户"+key+"频繁发评论",false);
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }

    /**
     * 点赞限流操作
     */
    public void doRateLimitThumb(String key) {
        //创建一个限流器，每5秒钟2次
        RRateLimiter rRateLimiter = redissonClient.getRateLimiter(key);
        rRateLimiter.trySetRate(RateType.OVERALL, 3, 5, RateIntervalUnit.SECONDS);

        //每当一个操作来了后，请求一个令牌
        boolean b = rRateLimiter.tryAcquire(1);
        if (!b) {
            MailUtil.send("3105755134@qq.com", "点赞限流告警", "这傻逼用户"+key+"频繁发评论",false);
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
