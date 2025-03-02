package com.hh.hhojbackendjudgeservice.mq;

import com.hh.hhojbackendcommon.constant.MqConstant;
import com.hh.hhojbackendjudgeservice.JudgeService;
import com.hh.hhojbackendmodel.entity.QuestionSubmit;
import com.hh.hhojbackendmodel.enums.QuestionSubmitStatusEnum;
import com.hh.hhojbackendserviceclient.exception.BusinessException;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @author 黄昊
 * @version 1.0
 **/
@Component
@Slf4j
public class RabbitmqConsumer {

    @Resource
    private JudgeService judgeService;

    // 指定程序监听的消息队列和确认机制
    @RabbitListener(queues = {MqConstant.NORMAL_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("receiveMessage message = {}", message);
        long questionSubmitId = Long.parseLong(message);
        try {
            QuestionSubmit questionSubmit = judgeService.doJudge(questionSubmitId);
            if (questionSubmit.getStatus()!= QuestionSubmitStatusEnum.WAITING.getValue()) {
                channel.basicAck(deliveryTag, false); // 成功处理消息，确认
            } else {
                channel.basicNack(deliveryTag, false, false); // 如果doJudge返回false，表示需要重新处理该消息
            }
        } catch (BusinessException e) {
            if (e.getMessage().equals("题目正在判题中")){
                channel.basicAck(deliveryTag, false); // 成功处理消息，确认
            }else {
                channel.basicNack(deliveryTag, false, false);
                throw new RuntimeException(e);
            }
        } catch (Throwable t) { // 捕获所有其他类型的异常
            channel.basicNack(deliveryTag, false, false); // 发生其他异常，重新入队
            throw t; // 重新抛出异常，让Lombok的@SneakyThrows处理
        }
    }

}

