package com.atguigu.hospital.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRequestHelperTest {

    private static final String SIGN_KEY = "hospital-sign-key-for-unit-test";

    @Test
    void validatesSignatureAndTimestampTogether() {
        Map<String, Object> valid = signedParams(System.currentTimeMillis());
        Map<String, Object> expired = signedParams(System.currentTimeMillis() - 6 * 60 * 1000L);

        assertTrue(HttpRequestHelper.isSignEquals(valid, SIGN_KEY));
        assertFalse(HttpRequestHelper.isSignEquals(expired, SIGN_KEY));
    }

    private Map<String, Object> signedParams(long timestamp) {
        Map<String, Object> params = new HashMap<>();
        params.put("timestamp", timestamp);
        params.put("sign", HttpRequestHelper.getSignSingle(SIGN_KEY));
        return params;
    }
}
