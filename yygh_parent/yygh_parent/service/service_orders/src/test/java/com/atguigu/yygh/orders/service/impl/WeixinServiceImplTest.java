package com.atguigu.yygh.orders.service.impl;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.enums.RefundStatusEnum;
import com.atguigu.yygh.model.order.PaymentInfo;
import com.atguigu.yygh.model.order.RefundInfo;
import com.atguigu.yygh.orders.service.PaymentInfoService;
import com.atguigu.yygh.orders.service.RefundInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class WeixinServiceImplTest {

    @Mock
    private PaymentInfoService paymentInfoService;

    @Mock
    private RefundInfoService refundInfoService;

    private WeixinServiceImpl weixinService;

    @BeforeEach
    void setUp() {
        weixinService = new WeixinServiceImpl();
        ReflectionTestUtils.setField(weixinService, "paymentInfoService", paymentInfoService);
        ReflectionTestUtils.setField(weixinService, "refundInfoService", refundInfoService);
        ReflectionTestUtils.setField(weixinService, "mockPaymentEnabled", true);
    }

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

    @Test
    void mockRefundCreatesSuccessfulRefundRecordWithoutWechatConfiguration() {
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(10L);
        paymentInfo.setOutTradeNo("trade-10");
        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setRefundStatus(RefundStatusEnum.UNREFUND.getStatus());
        when(paymentInfoService.getPaymentInfoByOrderId(10L)).thenReturn(paymentInfo);
        when(refundInfoService.savefundInfo(paymentInfo)).thenReturn(refundInfo);

        boolean result = weixinService.refund(10L);

        assertTrue(result);
        assertEquals(RefundStatusEnum.REFUND.getStatus(), refundInfo.getRefundStatus());
        assertEquals("MOCK-REFUND-trade-10", refundInfo.getTradeNo());
        verify(refundInfoService).updateById(any(RefundInfo.class));
    }
}
