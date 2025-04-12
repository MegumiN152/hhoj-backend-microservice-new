package com.hh.hhojbackendgateway.listener;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.exception.NacosException;
import com.google.common.hash.BloomFilter;
import com.hh.hhojbackendgateway.manager.BlacklistManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Configuration
@Slf4j
public class NacosConfigRefresh {
    private static final String BLACKLIST_DATA_ID = "gateway-blacklist.yaml";
    private static final String BLACKLIST_GROUP = "GATEWAY_GROUP";

    @Resource
    private NacosConfigManager nacosConfigManager;

    @Resource
    private BlacklistManager blacklistManager;

    @PostConstruct
    public void init() throws NacosException {
        ConfigService configService = nacosConfigManager.getConfigService();

        // 初始加载配置
        String initialConfig = configService.getConfig(BLACKLIST_DATA_ID, BLACKLIST_GROUP, 5000);
        blacklistManager.rebuildBloomFilter(initialConfig);

        // 添加监听器
        configService.addListener(
                BLACKLIST_DATA_ID,
                BLACKLIST_GROUP,
                new AbstractListener() {
                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        log.info("黑名单配置发生改变: {}", configInfo);
                        blacklistManager.rebuildBloomFilter(configInfo);
                    }
                }
        );
    }
}
