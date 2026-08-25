package com.atguigu.yygh.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 功能完善：统一保护管理端、用户 auth 接口和支付接口，并阻断外部访问 inner 接口。
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final SecretKey tokenSignKey;

    public AuthGlobalFilter(@Value("${yygh.jwt.secret:}") String jwtSecret) {
        if (!StringUtils.hasText(jwtSecret)
                || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("YYGH_JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        this.tokenSignKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        // 内部 Feign 接口应通过服务发现直连，禁止从公网网关绕过业务资源校验。
        if (path.contains("/inner/")) {
            return writeError(exchange, HttpStatus.FORBIDDEN, 50008, "禁止访问内部接口");
        }

        if (!requiresAuthentication(path)) {
            return chain.filter(exchange);
        }

        String token = firstText(request.getHeaders().getFirst("token"),
                request.getHeaders().getFirst("X-Token"));
        // 兼容旧管理端 info 请求的 query 参数，前端已同步改为标准 token 请求头。
        token = firstText(token, request.getQueryParams().getFirst("token"));
        if (!StringUtils.hasText(token)) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, 50008, "请先登录");
        }

        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(tokenSignKey)
                    .build()
                    .parseClaimsJws(token);
            Object rawUserId = claims.getBody().get("userId");
            if (!(rawUserId instanceof Number)) {
                return writeError(exchange, HttpStatus.UNAUTHORIZED, 50008, "登录凭证无效");
            }
            long userId = ((Number) rawUserId).longValue();
            if (isAdminPath(path) && userId != 0L) {
                return writeError(exchange, HttpStatus.FORBIDDEN, 50008, "没有管理权限");
            }

            // 将 X-Token 规范化为下游现有代码使用的 token 请求头。
            String authenticatedToken = token;
            ServerHttpRequest authenticatedRequest = request.mutate()
                    .headers(headers -> headers.set("token", authenticatedToken))
                    .build();
            return chain.filter(exchange.mutate().request(authenticatedRequest).build());
        } catch (Exception ex) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, 50008, "登录凭证已失效");
        }
    }

    private boolean requiresAuthentication(String path) {
        if ("/api/order/weixin/notify".equals(path)) {
            return false;
        }
        return path.contains("/auth/")
                || path.startsWith("/admin/")
                || "/user/hosp/info".equals(path)
                || path.startsWith("/api/order/weixin/");
    }

    private boolean isAdminPath(String path) {
        return path.startsWith("/admin/") || "/user/hosp/info".equals(path);
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private Mono<Void> writeError(ServerWebExchange exchange,
                                  HttpStatus status,
                                  int code,
                                  String message) {
        byte[] body = ("{\"success\":false,\"code\":" + code
                + ",\"message\":\"" + message + "\",\"data\":{}}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
