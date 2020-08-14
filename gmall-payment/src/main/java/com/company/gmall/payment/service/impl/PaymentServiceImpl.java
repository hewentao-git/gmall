package com.company.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.company.gmall.bean.PaymentInfo;
import com.company.gmall.config.ActiveMQUtil;
import com.company.gmall.enums.PaymentStatus;
import com.company.gmall.payment.mapper.PaymentInfoMapper;
import com.company.gmall.service.PaymentService;
import com.company.gmall.util.HttpClient;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    // 服务号Id
    @Value("${appid}")
    private String appid;
    // 商户号Id
    @Value("${partner}")
    private String partner;
    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;

    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Autowired
    private AlipayClient alipayClient;


    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfo) {
        return paymentInfoMapper.selectOne(paymentInfo);
    }

    @Override
    public void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo", outTradeNo);
        paymentInfoMapper.updateByExampleSelective(paymentInfo, example);
    }

    @Override
    public Map createNative(String orderId, String total_fee) {
        //1.创建参数
        Map<String, String> param = new HashMap();//创建参数
        param.put("appid", appid);//公众号
        param.put("mch_id", partner);//商户号
        param.put("nonce_str", WXPayUtil.generateNonceStr());//随机字符串
        param.put("body", "手机");//商品描述
        param.put("out_trade_no", orderId);//商户订单号
        param.put("total_fee", total_fee);//总金额（分）
        param.put("spbill_create_ip", "127.0.0.1");//IP
        param.put("notify_url", "http://order.gmall.com/list");//回调地址(随便写)
        param.put("trade_type", "NATIVE");//交易类型
        try {
            //2.生成要发送的xml
            String xmlParam = WXPayUtil.generateSignedXml(param, partnerkey);
            System.out.println(xmlParam);
            HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            client.setHttps(true);
            client.setXmlParam(xmlParam);
            client.post();
            //3.获得结果
            String result = client.getContent();
            System.out.println(result);
            Map<String, String> resultMap = WXPayUtil.xmlToMap(result);
            Map<String, String> map = new HashMap<>();
            map.put("code_url", resultMap.get("code_url"));//支付地址
            map.put("total_fee", total_fee);//总金额
            map.put("out_trade_no", orderId);//订单号
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {

        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;
        try {
            connection = activeMQUtil.getConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue payment_result_queue = session.createQueue("PAYMENT_RESULT_QUEUE");
            producer = session.createProducer(payment_result_queue);

            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("orderId", paymentInfo.getOrderId());
            activeMQMapMessage.setString("result", result);
            producer.send(activeMQMapMessage);
            session.commit();
        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            try {
                if (producer != null) {
                    producer.close();
                }
                if (session != null) {
                    session.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (JMSException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public boolean checkPayment(PaymentInfo paymentInfoQuery) {
        // 查询当前的支付信息
        PaymentInfo paymentInfo = getPaymentInfo(paymentInfoQuery);
        if (paymentInfo.getPaymentStatus() == PaymentStatus.PAID || paymentInfo.getPaymentStatus() == PaymentStatus.ClOSED) {
            return true;
        }

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        Map<String, Object> map = new HashMap<>();
        map.put("out_trade_no", paymentInfo.getOutTradeNo());
        String jsonString = JSON.toJSONString(map);
        request.setBizContent(jsonString);
        AlipayTradeQueryResponse response = null;

        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {

            if ("TRADE_SUCCESS".equals(response.getTradeStatus()) || "TRADE_FINISHED".equals(response.getTradeStatus())) {
                //  IPAD
                System.out.println("支付成功");
                // 改支付状态
                PaymentInfo paymentInfoUpd = new PaymentInfo();
                paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                updatePaymentInfo(paymentInfo.getOutTradeNo(), paymentInfoUpd);
                sendPaymentResult(paymentInfo, "success");
                return true;
            } else {
                System.out.println("支付失败");
                return false;
            }

        } else {
            System.out.println("支付失败");
            return false;
        }

    }

    /**
     * 延迟队列反复调用
     *
     * @param outTradeNo 单号
     * @param delaySec   延迟秒
     * @param checkCount 几次
     */
    @Override
    public void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount) {
        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;
        try {
            connection = activeMQUtil.getConnection();

            connection.start();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue payment_result_check_queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            producer = session.createProducer(payment_result_check_queue);

            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("outTradeNo", outTradeNo);
            mapMessage.setInt("delaySec", delaySec);
            mapMessage.setInt("checkCount", checkCount);

            //设置延迟时间
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delaySec * 1000);
            producer.send(mapMessage);
            session.commit();
        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            try {
                if (producer != null) {
                    producer.close();
                }
                if (session != null) {
                    session.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    // 关闭支付信息
    public  void  closePayment(String orderId){
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderId",orderId);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED);
        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
    }

}

