package com.hh.hhojbackendcommon.utils;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.*;
import java.util.Date;

public class JwtUtils {
    private static final String KEY = "hhoj_key_152";

    private static final Long EXPIRE_TIME=1000*60*60*24L;

    // 生成 Token（包含用户 ID 和角色）
    public static String generateToken(Long userId, String role) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(SignatureAlgorithm.HS256, KEY)
                .compact();
    }

    // 解析 Token
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(KEY)
                .parseClaimsJws(token)
                .getBody();
    }

}
