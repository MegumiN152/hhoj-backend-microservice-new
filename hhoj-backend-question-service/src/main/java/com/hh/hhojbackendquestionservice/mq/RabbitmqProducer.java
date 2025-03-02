package com.hh.hhojbackendquestionservice.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author 黄昊
 * @version 1.0
 **/
@Component
@Slf4j
public class RabbitmqProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String exchange, String routingKey, String message) {
        log.info("发送消息到mq,exchange:{},routingKey:{},message:{}", exchange, routingKey, message);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

}

