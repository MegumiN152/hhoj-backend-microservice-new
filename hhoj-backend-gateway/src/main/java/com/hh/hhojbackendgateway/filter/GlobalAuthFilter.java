package com.hh.hhojbackendgateway.filter;

import cn.hutool.core.text.AntPathMatcher;
import com.hh.hhojbackendcommon.utils.JwtUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author 黄昊
 * @version 1.0
 **/
@Component
public class GlobalAuthFilter implements GlobalFilter {
    private AntPathMatcher authPathMatcher=new AntPathMatcher();
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        //判断路径中是否包含 inner，只运行内部调用
        if(authPathMatcher.match("/**/inner/**",path)){
            return writeError(exchange.getResponse(), "无权限");
        }
        //2. 公开接口（如登录注册）放行
        if (path.contains("/api/user/login")||path.contains("/api/user/register")){
            return chain.filter(exchange);
        }
        //3.验证 jwt
        String token = request.getHeaders().getFirst("Authorization");
        if (StringUtils.isBlank(token)||!token.startsWith("Bearer ")){
            return writeError(exchange.getResponse(),"未提供token");
        }
        try {
            //4. 解析jwt
            token=token.substring(7);
            Map<String, Object> claims = JwtUtils.parseToken(token);
            Object userId = claims.get("userId");
            String userRole = (String)claims.get("userRole");

            //将用户信息添加到请求头中，传递给下游服务
            ServerHttpRequest newRequest = request.mutate()
                    .header("X-user-Id", userId.toString())
                    .header("X-user-Role", userRole)
                    .build();
            return chain.filter(exchange.mutate().request(newRequest).build());
        } catch (Exception e) {
            return writeError(exchange.getResponse(),"Token无效或已过期");
        }
    }
    private Mono<Void> writeError(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        DataBuffer buffer = response.bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
