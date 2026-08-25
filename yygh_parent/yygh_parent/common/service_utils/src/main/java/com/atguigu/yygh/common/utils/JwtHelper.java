package com.atguigu.yygh.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;


public class JwtHelper {

    //token过期时间固定值
    private static long tokenExpiration = 60 * 60 * 1000L;

    // 签名密钥只从运行环境读取，避免把生产凭据提交到版本库。
    private static final SecretKey tokenSignKey = createTokenSignKey();

    private static SecretKey createTokenSignKey() {
        String secret = System.getProperty("yygh.jwt.secret");
        if (!StringUtils.hasText(secret)) {
            secret = System.getenv("YYGH_JWT_SECRET");
        }
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("YYGH_JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    //生成token方法，参数用户id和用户名称
    public static String createToken(Long userId, String username) {
        String token = Jwts.builder().
                //设置分类
                        setSubject("USER_INFO").
                //设置生成token过期时间
                        setExpiration(new Date(System.currentTimeMillis() + tokenExpiration))
                //jwt负载内容，设置用户信息到token
                .claim("userId", userId)
                .claim("userName", username).
                //根据秘钥进行加密
                        signWith(tokenSignKey).
                //把生成token压缩
                        compressWith(CompressionCodecs.GZIP).
                compact();
        return token;
    }

    //根据token字符串，从token获取userid
    public static Long getUserId(String token) {
        if(StringUtils.isEmpty(token)) return null;
        Jws<Claims> claimsJws = Jwts.parser().setSigningKey(tokenSignKey).parseClaimsJws(token);
        Claims claims = claimsJws.getBody();
        Object userId = claims.get("userId");
        return userId instanceof Number ? ((Number) userId).longValue() : null;
    }

    //根据token字符串，从token获取UserName
    public static String getUserName(String token) {
        if(StringUtils.isEmpty(token)) return "";
        Jws<Claims> claimsJws
                = Jwts.parser().setSigningKey(tokenSignKey).parseClaimsJws(token);
        Claims claims = claimsJws.getBody();
        return (String)claims.get("userName");
    }

    public static void main(String[] args) {
        String token = JwtHelper.createToken(1L, "lucy");
        System.out.println(token);
        System.out.println(JwtHelper.getUserId(token));
        System.out.println(JwtHelper.getUserName(token));
    }
}
