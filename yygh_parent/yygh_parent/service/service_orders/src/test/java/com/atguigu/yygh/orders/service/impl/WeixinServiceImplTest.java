package com.atguigu.yygh.orders.service.impl;

import com.atguigu.yygh.common.exception.YyghException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeixinServiceImplTest {

    private final WeixinServiceImpl weixinService = new WeixinServiceImpl();

    @Test
    void convertsRealOrderAmountToFen() {
        String fen = ReflectionTestUtils.invokeMethod(weixinService,
                "toFen", new BigDecimal("100.00"));

        assertEquals("10000", fen);
    }

    @Test
    void rejectsAmountWithMoreThanTwoDecimals() {
        assertThrows(YyghException.class, () -> ReflectionTestUtils.invokeMethod(
                weixinService, "toFen", new BigDecimal("1.001")));
    }
}
