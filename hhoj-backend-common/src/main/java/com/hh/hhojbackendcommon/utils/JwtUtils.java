package com.hh.hhojbackendcommon.utils;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.*;
import java.util.Date;

public class JwtUtils {
    private static final String KEY = "01g1pxiEMURtD19PPqaLIfCKMh8hmS5dXy8OVUYspjVvWxjHVsLFUlJ1";

    private static final Long EXPIRE_TIME=1000*60*60*24L;

    // 生成 Token（包含用户 ID 和角色）
    public static String generateToken(Long userId, String role) {
        return Jwts.builder()
                .setSubject(role)
                .claim("userId", userId.toString())
                .claim("userRole", role)
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
