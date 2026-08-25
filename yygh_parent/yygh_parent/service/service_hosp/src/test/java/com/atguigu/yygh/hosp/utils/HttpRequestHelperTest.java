package com.atguigu.yygh.hosp.utils;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRequestHelperTest {

    private static final String SIGN_KEY = "hospital-sign-key-for-unit-test";

    @Test
    void acceptsValidFreshSignature() {
        Map<String, Object> params = signedParams(System.currentTimeMillis());

        assertTrue(HttpRequestHelper.isSignEquals(params, SIGN_KEY));
    }

    @Test
    void rejectsWrongOrExpiredSignature() {
        Map<String, Object> wrong = signedParams(System.currentTimeMillis());
        wrong.put("sign", "wrong-signature");
        Map<String, Object> expired = signedParams(System.currentTimeMillis() - 6 * 60 * 1000L);

        assertFalse(HttpRequestHelper.isSignEquals(wrong, SIGN_KEY));
        assertFalse(HttpRequestHelper.isSignEquals(expired, SIGN_KEY));
    }

    private Map<String, Object> signedParams(long timestamp) {
        Map<String, Object> params = new HashMap<>();
        params.put("hoscode", "10000");
        params.put("timestamp", timestamp);
        params.put("sign", HttpRequestHelper.getSignSingle(SIGN_KEY));
        return params;
    }
}
