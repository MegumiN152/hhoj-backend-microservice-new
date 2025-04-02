package com.hh.hhojbackendquestionservice.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 缓存清理任务
 */
@Slf4j
@Component
public class CacheClearTask {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 延迟双删策略
     * @param keys 要删除的缓存键
     * @param delay 延迟时间（毫秒）
     */
    @Async
    public void delayedDoubleDelete(String[] keys, long delay) {
        try {
            // 第一次删除
            for (String key : keys) {
                redisTemplate.delete(key);
            }

            // 等待一段时间，让主从同步完成
            Thread.sleep(delay);

            // 第二次删除
            for (String key : keys) {
                redisTemplate.delete(key);
            }

            log.info("延迟双删完成，keys: {}", keys);
        } catch (InterruptedException e) {
            log.error("延迟双删任务被中断", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 延迟删除单个key
     */
    @Async
    public void delayedDelete(String key, long delay) {
        try {
            // 第一次删除
            redisTemplate.delete(key);

            // 等待一段时间
            Thread.sleep(delay);

            // 第二次删除
            redisTemplate.delete(key);

            log.info("延迟双删完成，key: {}", key);
        } catch (InterruptedException e) {
            log.error("延迟双删任务被中断", e);
            Thread.currentThread().interrupt();
        }
    }
}
