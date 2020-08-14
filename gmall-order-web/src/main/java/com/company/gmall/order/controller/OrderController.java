package com.company.gmall.order.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.company.gmall.bean.*;
import com.company.gmall.config.LoginRequire;
import com.company.gmall.enums.OrderStatus;
import com.company.gmall.enums.ProcessStatus;
import com.company.gmall.service.*;
import com.company.gmall.util.HttpClientUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Controller
public class OrderController {

    @Reference
    private UserService userService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    @Reference
    private ManageService manageService;

    @Reference
    private PaymentService paymentService;

    // http://order.gmall.com/orderSplit?orderId=xxx&wareSkuMap=xxx
    @RequestMapping("orderSplit")
    @ResponseBody
    public String  orderSplit(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");
        // 返回的是子订单集合
        List<OrderInfo> orderInfoList = orderService.splitOrder(orderId,wareSkuMap);

        // 创建一个集合 来存储map
        ArrayList<Map> mapArrayList = new ArrayList<>();
        // 循环遍历
        for (OrderInfo orderInfo : orderInfoList) {

            Map map = orderService.initWareOrder(orderInfo);
            mapArrayList.add(map);
        }

        return JSON.toJSONString(mapArrayList);

    }

    // 查询订单信息
    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        PaymentInfo paymentInfoQuery = new PaymentInfo();
        paymentInfoQuery.setOrderId(orderId);
        boolean flag = paymentService.checkPayment(paymentInfoQuery);
        return ""+flag;
    }

    @RequestMapping("list")
    @LoginRequire
    public String getOrderList(HttpServletRequest request, Model model){
        String userId = (String) request.getAttribute("userId");
        List<OrderInfo> orderInfoList = orderService.getOrderInfoListByUserId(userId);
        model.addAttribute("orderInfoList",orderInfoList);
        return "list";
    }
    

    @RequestMapping("trade")
//    @ResponseBody // 第一个返回json 字符串，fastJson.jar 第二直接将数据显示到页面！
    @LoginRequire
    public String trade(HttpServletRequest request, Model model) {
        String userId = (String) request.getAttribute("userId");

        List<UserAddress> userAddressList = userService.getUserAddressList(userId);
//
        model.addAttribute("userAddressList", userAddressList);

        // 展示送货清单：
        // 数据来源：勾选的购物车！user:userId:checked！
        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);

        // 声明一个集合来存储订单明细
        ArrayList<OrderDetail> orderDetailArrayList = new ArrayList<>();
        // 将集合数据赋值OrderDetail
        for (CartInfo cartInfo : cartInfoList) {
            OrderDetail orderDetail = new OrderDetail();

            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());

            orderDetailArrayList.add(orderDetail);
        }

        // 总金额：
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailArrayList);
        // 调用计算总金额的方法  {totalAmount}
        orderInfo.sumTotalAmount();

        model.addAttribute("totalAmount", orderInfo.getTotalAmount());
        // 保存送货清单集合
        model.addAttribute("orderDetailArrayList", orderDetailArrayList);
        String tradeNo = orderService.getTradeNo(userId);
        model.addAttribute("tradeNo", tradeNo);
        return "trade";
    }

    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");

        // 判断是否是重复提交
        // 先获取页面的流水号
        String tradeNo = request.getParameter("tradeNo");
        // 调用比较方法
        boolean result1 = orderService.checkTradeCode(userId, tradeNo);
        // 是重复提交
        if (!result1){
            request.setAttribute("errMsg","订单已提交，不能重复提交！");
            return "tradeFail";
        }
        // 初始化参数
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfo.sumTotalAmount();
        orderInfo.setUserId(userId);
        // 校验，验价
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        for (OrderDetail orderDetail : orderDetailList) {
            SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
            int compareTo = skuInfo.getPrice().compareTo(orderDetail.getOrderPrice());
            if (compareTo != 0){
                request.setAttribute("errMsg", orderDetail.getSkuName() + "价格不匹配，请重新下单！");
                cartService.loadCartCache(userId);
                return "tradeFail";
            }
            // 从订单中去购物skuId，数量
            boolean result = checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!result) {
                request.setAttribute("errMsg", orderDetail.getSkuName() + "库存不足，请重新下单！");
                return "tradeFail";
            }
        }
        String orderId = orderService.saveOrder(orderInfo);
        for (OrderDetail orderDetail : orderDetailList) {

            cartService.del(orderDetail.getSkuId(),userId);
        }
        // 删除tradeNo
        orderService.delTradeNo(userId);
        return "redirect://payment.gmall.com/index?orderId=" + orderId;

    }

    private boolean checkStock(String skuId, Integer skuNum) {
        // 验证库存
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        if ("1".equals(result)) {
            return true;
        } else {
            return false;
        }
    }

}
