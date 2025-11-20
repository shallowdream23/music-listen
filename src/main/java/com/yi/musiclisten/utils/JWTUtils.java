package com.yi.musiclisten.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JWTUtils {
    // TOKEN的有效期30分钟（单位毫秒）
    private static final int TOKEN_TIME_OUT = 1000 * 60 * 30 ;
    // 加密KEY
    private static final String TOKEN_ENCRY_KEY = "MDk4ZjZiY2Q0NjIxZDM3M2NhZGU0ZTgzMjYyN2I0ZjY";
    /**
     * 生成令牌
     * @param claims 私有信息，识别身份
     * @return
     */
    public static String generateToken(Map<String,Object> claims){
        // 设置头部
        Map<String,Object> header = new HashMap<>();
        header.put("alg","HS256");
        header.put("typ","JWT");
        // 签发时间
        Long created = System.currentTimeMillis();
        Date issued = new Date(created);
        // 过期时间
        long expired = created + TOKEN_TIME_OUT;
        Date expires = new Date(expired);
        return Jwts.builder()
                .setHeader(header)
                .setClaims(claims)
                .setExpiration(expires)
                .setIssuedAt(issued)
                .signWith(SignatureAlgorithm.HS256,TOKEN_ENCRY_KEY)//设置签名
                .compact();
    }

    /**
     * 解析令牌
     * @param token
     * @return
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(TOKEN_ENCRY_KEY)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证令牌
     * @param token
     * @return
     */
    public static boolean isExpired(String token) {
        if(token == null) return false;
        try {
            parseToken(token);
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
