package com.company.gmall.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.company.gmall.enums.ProcessStatus;
import com.company.gmall.service.OrderService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
@Component
public class OrderConsumer {

    @Reference
    private OrderService orderService;

    @JmsListener(destination =  "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage) throws JMSException {
        //通过mapMessage获取
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        System.out.println(orderId + result);
        if ("success".equals(result)){
            //支付成功 更新订单状态
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);//已支付
            //发送消息给库存
            orderService.sendOrderStatus(orderId);
            //更改订单状态
            orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);//已通知仓库
        }
    }

    @JmsListener(destination =  "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        //通过mapMessage获取
        String orderId = mapMessage.getString("orderId");
        String status = mapMessage.getString("status");
        if("DEDUCTED".equals(status)){
            orderService.updateOrderStatus(  orderId , ProcessStatus.WAITING_DELEVER);
        }else{
            orderService.updateOrderStatus(  orderId , ProcessStatus.STOCK_EXCEPTION);
        }

    }
}
