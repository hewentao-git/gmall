package com.company.gmall.payment.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.company.gmall.bean.PaymentInfo;
import com.company.gmall.service.PaymentService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class PaymentConsumer {
    @Reference
    private PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE", containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {

        // 获取消息队列中的数据
        String outTradeNo = mapMessage.getString("outTradeNo");
        int delaySec = mapMessage.getInt("delaySec");
        int checkCount = mapMessage.getInt("checkCount");

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        PaymentInfo paymentInfo1 = paymentService.getPaymentInfo(paymentInfo);

        boolean flag = paymentService.checkPayment(paymentInfo1);
        System.out.println("检查结果：" + flag);
        if (!flag && checkCount != 0) {
            // 还需要继续检查
            System.out.println("检查的次数：" + checkCount);
            paymentService.sendDelayPaymentResult(outTradeNo, delaySec, checkCount - 1);
        }


    }

}
