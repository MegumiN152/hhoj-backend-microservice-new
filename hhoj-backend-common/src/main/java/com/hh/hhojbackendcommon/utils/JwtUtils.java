package com.hh.hhojbackendcommon.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;

import java.util.Map;
import java.util.Date;

public class JwtUtils {
    private static final String KEY = "hhoj_key_152";

    //接收业务数据,生成token并返回
    public static String getToken(Map<String, Object> claims) {
        return JWT.create()
                .withClaim("claims", claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 12))
                .sign(Algorithm.HMAC256(KEY));
    }

    //接收token,验证token,并返回业务数据
    public static Map<String, Object> parseToken(String token) {
        try {
            Map<String, Object> claims = JWT.require(Algorithm.HMAC256(KEY))
                    .build()
                    .verify(token)
                    .getClaim("claims")
                    .asMap();
            return claims;
        } catch (JWTVerificationException e) {
            // 处理 token 验证异常
            throw new RuntimeException("Invalid token", e);
        }
    }

}
