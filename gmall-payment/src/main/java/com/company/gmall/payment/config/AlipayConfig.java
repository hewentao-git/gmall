package com.company.gmall.payment.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.FileWriter;
import java.io.IOException;

@Configuration
@PropertySource("classpath:alipay.properties")
public class AlipayConfig implements InitializingBean {

    // 应用ID,您的APPID，收款账号既是您的APPID对应支付宝账号
    public static String app_id;

    // 商户私钥，您的PKCS8格式RSA2私钥
    public static String merchant_private_key;

    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    public static String alipay_public_key;

    // 服务器异步通知页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    public static String notify_url;

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    public static String return_url;

    // 签名方式
    public static String sign_type;

    // 字符编码格式
    public static String charset;

    // 支付宝网关
    public static String gatewayUrl;

    // 支付宝网关
    public static String log_path = "C:\\";

    public final static String format="json";

    /**
     * 写日志，方便测试（看网站需求，也可以改成把记录存入数据库）
     *
     * @param sWord 要写入日志里的文本内容
     */
    public static void logResult(String sWord) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(log_path + "alipay_log_" + System.currentTimeMillis() + ".txt");
            writer.write(sWord);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Value("${alipay.app_id}")
    public String appid;
    @Value("${alipay.merchant_private_key}")
    public String merchantprivatekey;
    @Value("${alipay.alipay_public_key}")
    public String alipaypublickey;
    @Value("${alipay.notify_url}")
    public String notifyurl;
    @Value("${alipay.return_url}")
    public String returnurl;
    @Value("${alipay.sign_type}")
    public String signtype;
    @Value("${alipay.charset}")
    public String charset2;
    @Value("${alipay.gatewayUrl}")
    public String gatewayUrl2;

    @Override
    public void afterPropertiesSet() throws Exception {
// 为所有的静态属性赋值为Spring获取到的值
        app_id = appid;
        merchant_private_key = merchantprivatekey;
        alipay_public_key = alipaypublickey;
        notify_url = notifyurl;
        return_url = returnurl;
        sign_type = signtype;
        charset = charset2;
        gatewayUrl = gatewayUrl2;

    }


    @Bean
    public AlipayClient alipayClient(){
        AlipayClient alipayClient=new DefaultAlipayClient(gatewayUrl,app_id,merchant_private_key,format,charset, alipay_public_key,sign_type );
        return alipayClient;
    }

}

