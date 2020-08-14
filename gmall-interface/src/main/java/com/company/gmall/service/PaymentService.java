package com.company.gmall.service;

import com.company.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    void savePaymentInfo(PaymentInfo paymentInfo);

    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd);

    Map createNative(String orderId, String total_fee);

    void sendPaymentResult(PaymentInfo paymentInfo,String result);

    /**
     * 据out_trade_no查询交易记录
     * @param paymentInfoQuery
     * @return
     */
    boolean checkPayment(PaymentInfo paymentInfoQuery);

    void sendDelayPaymentResult(String outTradeNo,int delaySec ,int checkCount);

    void closePayment(String id);
}
