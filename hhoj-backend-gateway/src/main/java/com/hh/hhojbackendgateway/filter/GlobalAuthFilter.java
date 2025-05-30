package com.hh.hhojbackendgateway.filter;

import cn.hutool.core.text.AntPathMatcher;
import com.hh.hhojbackendcommon.utils.BlackIpUtils;
import com.hh.hhojbackendcommon.utils.JwtUtils;
import com.hh.hhojbackendcommon.utils.NetUtils;
import com.hh.hhojbackendgateway.manager.BlacklistManager;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * @author 黄昊
 * @version 1.0
 **/
@Component
public class GlobalAuthFilter implements GlobalFilter {
    private AntPathMatcher authPathMatcher = new AntPathMatcher();
    // 不需要验证token的路径
    private static final String[] WHITE_LIST = {
            "/api/user/login",
            "/api/user/get/login",
            "/api/user/register",
            "/api/user/logout",
            "/api/doc.html",
            "/api/v3/api-docs",
            "/api/v2/api-docs",
            "/api/swagger-resources",
            "/api/swagger-ui.html",
            "/api/webjars/**",
            "/api/comment/v2/**",
            "/api/question/v2/**",
            "/api/judge/v2/**",
            "/api/user/v2/**",
    };
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private BlacklistManager blacklistManager;
    /**
     * 判断是否为白名单路径
     * @param path 请求路径
     * @return 是否在白名单中
     */
    private boolean isWhiteListPath(String path) {
        for (String whitePath : WHITE_LIST) {
            if (authPathMatcher.match(whitePath, path)) {
                return true;
            }
        }
        return false;
    }
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        // 获取请求对应的ip
        String ipAddress = getClientIp(request);
        if (blacklistManager.isBlockedIp(ipAddress)){
            return writeError(exchange.getResponse(),"黑名单IP，禁止访问",HttpStatus.FORBIDDEN);
        }
        //判断路径中是否包含 inner，只运行内部调用
        if (authPathMatcher.match("/**/inner/**", path)) {
            return writeError(exchange.getResponse(), "无权限");
        }
        //2. 公开接口（如登录注册）放行
        if (isWhiteListPath(path)) {
            return chain.filter(exchange);
        }
        //3.验证 jwt
        String token = request.getHeaders().getFirst("Authorization");
        if (StringUtils.isBlank(token) || !token.startsWith("Bearer ")) {
            return writeError(exchange.getResponse(), "未提供token");
        }
        try {
            token = token.substring(7);
            // 解析 JWT 并验证
            // 新增：检查 Token 是否在黑名单中
            if (stringRedisTemplate.hasKey("jwt:blacklist:" + token)) {
                return writeError(exchange.getResponse(), "Token 已失效");
            }
            Claims claims = JwtUtils.parseToken(token);
            Long userId = Long.parseLong(claims.get("userId", String.class));
            String userRole = claims.get("userRole", String.class);

            //将用户信息添加到请求头中，传递给下游服务
            ServerHttpRequest newRequest = request.mutate()
                    .header("X-user-Id", userId.toString())
                    .header("X-user-Role", userRole)
                    .build();
            return chain.filter(exchange.mutate().request(newRequest).build());
        } catch (Exception e) {
            return writeError(exchange.getResponse(), "Token无效或已过期");
        }
    }

    private Mono<Void> writeError(ServerHttpResponse response, String message) {
       return writeError(response,message,HttpStatus.UNAUTHORIZED);
    }
    private Mono<Void> writeError(ServerHttpResponse response, String message,HttpStatus httpStatus) {
        response.setStatusCode(httpStatus);
        DataBuffer buffer = response.bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
    private String getClientIp(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();

        // 优先级1：X-Forwarded-For（需处理多级代理）
        String xff = headers.getFirst("X-Forwarded-For");
        if (StringUtils.isNotEmpty(xff)) {
            String[] ips = xff.split(",");
            // 取第一个非 unknown 的IP
            for (String ip : ips) {
                ip = ip.trim();
                if (!"unknown".equalsIgnoreCase(ip)) {
                    return ip;
                }
            }
        }

        // 优先级2：X-Real-IP
        String xRealIp = headers.getFirst("X-Real-IP");
        if (StringUtils.isNotEmpty(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        // 优先级3：Proxy-Client-IP（针对Apache服务）
        String proxyClientIp = headers.getFirst("Proxy-Client-IP");
        if (StringUtils.isNotEmpty(proxyClientIp) && !"unknown".equalsIgnoreCase(proxyClientIp)) {
            return proxyClientIp;
        }

        // 优先级4：WL-Proxy-Client-IP（WebLogic）
        String wlProxyClientIp = headers.getFirst("WL-Proxy-Client-IP");
        if (StringUtils.isNotEmpty(wlProxyClientIp) && !"unknown".equalsIgnoreCase(wlProxyClientIp)) {
            return wlProxyClientIp;
        }

        // 最终回退：从TCP连接获取（仅在没有代理时有效）
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }
}
