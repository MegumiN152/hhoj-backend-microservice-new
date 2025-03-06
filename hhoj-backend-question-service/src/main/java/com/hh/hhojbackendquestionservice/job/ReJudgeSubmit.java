package com.hh.hhojbackendquestionservice.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hh.hhojbackendcommon.common.ErrorCode;
import com.hh.hhojbackendcommon.constant.MqConstant;
import com.hh.hhojbackendmodel.entity.QuestionSubmit;
import com.hh.hhojbackendmodel.enums.QuestionSubmitStatusEnum;
import com.hh.hhojbackendquestionservice.exception.BusinessException;
import com.hh.hhojbackendquestionservice.mq.RabbitmqProducer;
import com.hh.hhojbackendquestionservice.service.QuestionSubmitService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ReJudgeSubmit {
    @Resource
    private RabbitmqProducer rabbitmqProducer;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Scheduled(cron = "0 0/10 * * * ?")
    public void reJudge() {
        QueryWrapper<QuestionSubmit> questionSubmitQueryWrapper = new QueryWrapper<>();
        questionSubmitQueryWrapper.eq("status", QuestionSubmitStatusEnum.JUDGING.getValue());
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        List<QuestionSubmit> judgingList = questionSubmitService.list(questionSubmitQueryWrapper);
        List<QuestionSubmit> reJudgeList = judgingList.stream().filter(questionSubmit ->
                {
                    // 假设 getCreateTime 返回的是 java.util.Date 类型
                    // 首先转换为 LocalDateTime
                    LocalDateTime createTime = questionSubmit.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

                    // 计算两个时间之间的分钟差
                    long minutesDifference = ChronoUnit.MINUTES.between(createTime, now);

                    // 过滤出创建时间超过3分钟的记录
                    return minutesDifference > 3;
                }).peek(questionSubmit -> questionSubmit.setStatus(QuestionSubmitStatusEnum.FAILED.getValue()))
                .collect(Collectors.toList());
        //更新这些未被消费的题目提交状态为fail
        if (!reJudgeList.isEmpty()) {
            for (QuestionSubmit qs : reJudgeList) {
                // 使用MyBatis-Plus的updateById或其他方法来更新每个实体的状态
                boolean success = questionSubmitService.updateById(qs); // 确保您的服务实现了IService接口，并且有updateById方法
                if (!success) {
                    // 处理更新失败的情况
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "定时重新判题更新状态失败");
                }
            }
        }
        for (QuestionSubmit questionSubmit : reJudgeList) {
            rabbitmqProducer.sendMessage(MqConstant.EXCHANGE_NAME, MqConstant.NORMAL_ROUTING_KEY, String.valueOf(questionSubmit.getId()));
        }
    }
}
