package com.hh.hhojbackendgateway.manager;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RefreshScope
public class BlacklistManager {
    // 布隆过滤器参数（100万容量，1%误判率）
    private static final int EXPECTED_INSERTIONS = 1_000_000;
    private static final double FPP = 0.01;

    private BloomFilter<String> ipBloomFilter;

    @Value("${blacklist.ips:[]}")
    private List<String> blacklistIps;

    @PostConstruct
    public void init() {
        rebuildBloomFilter();
    }

    public synchronized void rebuildBloomFilter() {
        BloomFilter<String> newFilter = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8),
            EXPECTED_INSERTIONS,
            FPP
        );
        blacklistIps.forEach(newFilter::put);
        ipBloomFilter = newFilter;
        log.info("黑名单已更新，IP数量: {},ip->{}", blacklistIps.size(),blacklistIps.toString());
    }

    public boolean isBlockedIp(String ip) {
        return ipBloomFilter.mightContain(ip);
    }
    public void rebuildBloomFilter(String configContent) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(configContent);
            // 确保 "blacklist" 是一个 Map
            Object blacklistObj = config.get("blacklist");
            if (!(blacklistObj instanceof Map)) {
                log.error("黑名单配置解析失败：'blacklist' 不是一个有效的 Map");
                return;
            }

            Map<String, Object> blacklistMap = (Map<String, Object>) blacklistObj;

            // 确保 "ips" 是一个 List
            Object ipsObj = blacklistMap.getOrDefault("ips", Collections.emptyList());
            if (!(ipsObj instanceof List)) {
                log.error("黑名单配置解析失败：'ips' 不是一个有效的 List");
                return;
            }

            List<String> ips = (List<String>) ipsObj;
            synchronized (this) {
                BloomFilter<String> newFilter = BloomFilter.create(
                        Funnels.stringFunnel(StandardCharsets.UTF_8),
                        EXPECTED_INSERTIONS,
                        FPP
                );
                ips.forEach(newFilter::put);
                ipBloomFilter = newFilter;
                log.info("黑名单重建成功，IP数量: {}", ips.size());
            }
        } catch (Exception e) {
            log.error("黑名单配置解析失败", e);
        }
    }
}