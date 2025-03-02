package com.hh.hhojbackendquestionservice.config;

import io.github.briqt.spark4j.SparkClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 黄昊
 * @version 1.0
 **/
@Configuration
@ConfigurationProperties(prefix = "xunfei.client")
@Data
public class XinHuoConfig {
    private String appid;
    private String apiSecret;
    private String apiKey;
    @Bean
    public SparkClient sparkClient(){
        SparkClient sparkClient = new SparkClient();
        sparkClient.apiKey=apiKey;
        sparkClient.appid=appid;
        sparkClient.apiSecret=apiSecret;
        return sparkClient;
    }
}
