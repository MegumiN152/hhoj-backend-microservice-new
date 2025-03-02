package com.hh.hhojbackendcommon.constant;

/**
 * @author 黄昊
 * @version 1.0
 **/
public interface MqConstant {
    /**
     * 声明正常交换机
     */
    String EXCHANGE_NAME = "code_exchange";

    /**
     * 正常队列
     */
    String NORMAL_QUEUE_NAME = "code_queue";

    /**
     * 正常路由
     */
    String NORMAL_ROUTING_KEY = "code_routingKey";

    /**
     * 声明死信交换机
     */
    String DEAD_LETTER_EXCHANGE = "dlx_oj_exchange";

    /**
     * 声明死信队列
     */
    String DEAD_LETTER_QUEUE = "dlx_oj_queue";

    /**
     * 声明死信路由
     */
    String DEAD_LETTER_ROUTING_KEY = "dlx_routingKey";
}
