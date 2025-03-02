package com.hh.hhojbackendquestionservice.mq;

import com.hh.hhojbackendcommon.constant.MqConstant;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;

/**
 * 用于创建测试程序用到的交换机和队列（只用在程序启动前执行一次）
 */
@Slf4j
@Component
public class InitRabbitMqBean {

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;
    @PostConstruct
    public void init() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPassword(password);
            factory.setUsername(username);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            //声明正常交换机
            channel.exchangeDeclare(MqConstant.EXCHANGE_NAME, "direct");

            //声明死信交换机
            channel.exchangeDeclare(MqConstant.DEAD_LETTER_EXCHANGE, "direct");

            //声明死信队列
            channel.queueDeclare(MqConstant.DEAD_LETTER_QUEUE, true, false, false, null);

            //绑定死信队列到死信交换机
            channel.queueBind(MqConstant.DEAD_LETTER_QUEUE, MqConstant.DEAD_LETTER_EXCHANGE, MqConstant.DEAD_LETTER_ROUTING_KEY);

            // 创建队列，随机分配一个队列名称
            //设置死信交换机和路由键
            HashMap<String, Object> args = new HashMap<>();
            args.put("x-dead-letter-exchange", MqConstant.DEAD_LETTER_EXCHANGE);
            args.put("x-dead-letter-routing-key", MqConstant.DEAD_LETTER_ROUTING_KEY);
            channel.queueDeclare(MqConstant.NORMAL_QUEUE_NAME, true, false, false, args);

            //绑定正常队列到正常交换机
            channel.queueBind(MqConstant.NORMAL_QUEUE_NAME, MqConstant.EXCHANGE_NAME, MqConstant.NORMAL_ROUTING_KEY);
            log.info("消息队列启动成功");
        } catch (Exception e) {
            log.error("消息队列启动失败", e);
        }
    }
}
