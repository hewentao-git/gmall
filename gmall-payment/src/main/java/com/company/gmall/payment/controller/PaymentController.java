package com.company.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.company.gmall.bean.OrderInfo;
import com.company.gmall.bean.PaymentInfo;
import com.company.gmall.config.LoginRequire;
import com.company.gmall.enums.PaymentStatus;
import com.company.gmall.payment.config.AlipayConfig;
import com.company.gmall.service.OrderService;
import com.company.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;

    @Reference
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient;

    @RequestMapping("wx/submit")
    @LoginRequire
    @ResponseBody
    public Map createNative(String orderId){
        //  // 做一个判断：支付日志中的订单支付状态 如果是已支付，则不生成二维码直接重定向到消息提示页面！
        // 调用服务层数据
        // 第一个参数是订单Id ，第二个参数是多少钱，单位是分
        orderId = UUID.randomUUID().toString().replace("-","");
        Map map = paymentService.createNative(orderId, "1");
        System.out.println(map.get("code_url"));
        // data = map
        return map;
    }


    @RequestMapping(value = "/alipay/submit", method = RequestMethod.POST)
    @LoginRequire
    @ResponseBody
    public String submitPayment(HttpServletRequest request, HttpServletResponse response) {
        // 获取订单Id
        String orderId = request.getParameter("orderId");
        // 取得订单信息
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        // 保存支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);

        // 保存信息
        paymentService.savePaymentInfo(paymentInfo);

        // 支付宝参数

        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        alipayRequest.setReturnUrl(AlipayConfig.return_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_url);//在公共参数中设置回跳和通知地址

        // 声明一个Map
        Map<String, Object> bizContnetMap = new HashMap<>();
        bizContnetMap.put("out_trade_no", paymentInfo.getOutTradeNo());
        bizContnetMap.put("body", orderInfo.getOrderComment());
        bizContnetMap.put("product_code", "FAST_INSTANT_TRADE_PAY");
        bizContnetMap.put("subject", paymentInfo.getSubject());
        bizContnetMap.put("total_amount", paymentInfo.getTotalAmount());
        // 将map变成json
        String Json = JSON.toJSONString(bizContnetMap);
        alipayRequest.setBizContent(Json);
        String form = "";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");
        // 代码追后面 15秒执行一次，总共需要执行3次。
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),15,3);
        return form;
    }

    @RequestMapping(value = "/alipay/callback/return", method = RequestMethod.GET)
    public String callbackReturn() {
        System.err.println("同步回调");
        return "redirect:http://order.gmall.com/list";
    }

    @RequestMapping(value = "/alipay/callback/notify", method = RequestMethod.POST)
    @ResponseBody
    public String paymentNotify(@RequestParam Map<String, String> paramMap, HttpServletRequest request) {
        try {
            boolean flag = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, "utf-8", AlipayConfig.sign_type);
            if (!flag) {
                return "fail";
            }
            // 判断结束
            String trade_status = paramMap.get("trade_status");
            System.err.println(trade_status);
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {
                // 查单据是否处理
                String out_trade_no = paramMap.get("out_trade_no");
                System.err.println(out_trade_no);
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setOutTradeNo(out_trade_no);
                PaymentInfo paymentInfoHas = paymentService.getPaymentInfo(paymentInfo);

                if (paymentInfoHas.getPaymentStatus() == PaymentStatus.PAID || paymentInfoHas.getPaymentStatus() == PaymentStatus.ClOSED) {
                    return "fail";
                }
                // 修改
                PaymentInfo paymentInfoUpd = new PaymentInfo();
                // 设置状态
                paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                // 设置创建时间
                paymentInfoUpd.setCallbackTime(new Date());
                // 设置内容
                paymentInfoUpd.setCallbackContent(paramMap.toString());
                paymentService.updatePaymentInfo(out_trade_no, paymentInfoUpd);
                //发送消息队列给订单
                paymentService.sendPaymentResult(paymentInfoHas,"success");
                return "success";

            }
            return "fail";
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return "fail";
        }
    }

    // 发送验证
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo,@RequestParam("result") String result){
        paymentService.sendPaymentResult(paymentInfo,result);
        return "sent payment result";
    }

    @RequestMapping("index")
    @LoginRequire
    public String index(HttpServletRequest request, Model model) {
        // 获取订单的id
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        model.addAttribute("orderId", orderId);
        model.addAttribute("totalAmount", orderInfo.getTotalAmount());
        return "index";
    }
}

