package com.atguigu.yygh.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthGlobalFilterTest {

    private static final String SECRET = "unit-test-jwt-secret-with-at-least-32-bytes";
    private final AuthGlobalFilter filter = new AuthGlobalFilter(SECRET);

    @Test
    void blocksExternalInnerEndpoint() {
        AtomicBoolean chained = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange("/api/user/patient/inner/getPatientInfoById/1", null);

        StepVerifier.create(filter.filter(exchange, chain(chained))).verifyComplete();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(chained.get());
    }

    @Test
    void authenticatesUserEndpointAndNormalizesTokenHeader() {
        AtomicBoolean chained = new AtomicBoolean(false);
        String token = token(7L);
        MockServerWebExchange exchange = exchange("/api/order/orderInfo/auth/1/10", token);

        StepVerifier.create(filter.filter(exchange, requestExchange -> {
            chained.set(true);
            assertEquals(token, requestExchange.getRequest().getHeaders().getFirst("token"));
            return Mono.empty();
        })).verifyComplete();

        assertTrue(chained.get());
    }

    @Test
    void rejectsNonAdminTokenOnAdminEndpoint() {
        AtomicBoolean chained = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange("/admin/hosp/hospital/1/10", token(7L));

        StepVerifier.create(filter.filter(exchange, chain(chained))).verifyComplete();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(chained.get());
    }

    private MockServerWebExchange exchange(String path, String token) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get(path);
        if (token != null) {
            builder.header("X-Token", token);
        }
        return MockServerWebExchange.from(builder.build());
    }

    private GatewayFilterChain chain(AtomicBoolean chained) {
        return exchange -> {
            chained.set(true);
            return Mono.empty();
        };
    }

    private String token(long userId) {
        return Jwts.builder()
                .setSubject("USER_INFO")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
                .claim("userId", userId)
                .claim("userName", "test-user")
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
