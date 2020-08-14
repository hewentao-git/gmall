package com.company.gmall.service;

import com.company.gmall.bean.CartInfo;
import com.company.gmall.bean.OrderInfo;
import com.company.gmall.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {

    String getTradeNo(String userId);

    String saveOrder(OrderInfo orderInfo);

    boolean checkTradeCode(String userId, String tradeNo);

    void delTradeNo(String userId);


    OrderInfo getOrderInfo(String orderId);


    List<OrderInfo> getOrderInfoListByUserId(String userId);

    void updateOrderStatus(String orderId, ProcessStatus paid);

    void sendOrderStatus(String orderId);

    List<OrderInfo> getExpiredOrderList();

    void execExpiredOrder(OrderInfo orderInfo);

    Map initWareOrder(OrderInfo orderInfo);

    List<OrderInfo> splitOrder(String orderId, String wareSkuMap);
}
