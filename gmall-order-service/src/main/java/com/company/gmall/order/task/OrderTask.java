package com.company.gmall.order.task;

import com.alibaba.dubbo.config.annotation.Reference;
import com.company.gmall.bean.OrderInfo;
import com.company.gmall.service.OrderService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@EnableScheduling
@Component
public class OrderTask {

    @Reference
    private OrderService orderService;

    //cron表示人物启动规则
    @Scheduled(cron = "5 * * * * ?")
    public void test01(){
        System.out.println(Thread.currentThread().getName() + "001");
    }

    //cron表示人物启动规则
    @Scheduled(cron = "0/5 * * * * ?")
    public void test02(){
        System.out.println(Thread.currentThread().getName() + "002");
    }

    //cron表示人物启动规则
    @Scheduled(cron = "0/20 * * * * ?")
    public  void checkOrder(){
        System.out.println("开始处理过期订单");
        long starttime = System.currentTimeMillis();
        List<OrderInfo> expiredOrderList = orderService.getExpiredOrderList();
        for (OrderInfo orderInfo : expiredOrderList) {
            // 处理未完成订单
            orderService.execExpiredOrder(orderInfo);
        }
        long costtime = System.currentTimeMillis() - starttime;
        System.out.println("一共处理"+expiredOrderList.size()+"个订单 共消耗"+costtime+"毫秒");
    }

}
