package com.company.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.company.gmall.bean.CartInfo;
import com.company.gmall.bean.OrderDetail;
import com.company.gmall.bean.OrderInfo;
import com.company.gmall.config.ActiveMQUtil;
import com.company.gmall.config.RedisUtil;
import com.company.gmall.enums.OrderStatus;
import com.company.gmall.enums.ProcessStatus;
import com.company.gmall.order.mapper.OrderDetailMapper;
import com.company.gmall.order.mapper.OrderInfoMapper;
import com.company.gmall.service.OrderService;
import com.company.gmall.service.PaymentService;
import com.company.gmall.util.HttpClientUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Reference
    private PaymentService paymentService;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {
        // 数据不完整！总金额，订单状态，第三方交易编号，创建时间，过期时间，进程状态
        // 总金额
        orderInfo.sumTotalAmount();
        // 创建时间
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        // 第三方交易编号
        String outTradeNo = "COMPANY" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        // 创建时间
        orderInfo.setCreateTime(new Date());
        // 过期时间 +1
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());

        // 进程状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        // 只保存了一份订单
        orderInfoMapper.insertSelective(orderInfo);

        // 订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            // 设置orderId
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }

        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 定义一个流水号
        String tradeNo = UUID.randomUUID().toString();
        // String类型
        jedis.set(tradeNoKey, tradeNo);

        jedis.close();

        return tradeNo;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        // 获取缓存的流水号
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 获取数据
        String tradeNo = jedis.get(tradeNoKey);
        // 关闭
        jedis.close();
        return tradeCodeNo.equals(tradeNo);
    }

    @Override
    public void delTradeNo(String userId) {
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 删除
        jedis.del(tradeNoKey);
        jedis.close();

    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {

        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        // 将orderDetai 放入orderInfo 中
        orderInfo.setOrderDetailList(orderDetailMapper.select(orderDetail));
        return orderInfo;

    }

    @Override
    public List<OrderInfo> getOrderInfoListByUserId(String userId) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setUserId(userId);
        return orderInfoMapper.select(orderInfo);
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus paid) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(paid);
        orderInfo.setOrderStatus(paid.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public void sendOrderStatus(String orderId) {
        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;
        try {
            connection = activeMQUtil.getConnection();

            String orderJson = initWareOrder(orderId);
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");

            producer = session.createProducer(order_result_queue);
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            activeMQTextMessage.setText(orderJson);
            producer.send(activeMQTextMessage);
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
    public List<OrderInfo> getExpiredOrderList() {
        // 扫描过期订单方法

        Example example = new Example(OrderInfo.class);
        example.createCriteria().andLessThan("expireTime", new Date()).andEqualTo("processStatus", ProcessStatus.UNPAID);
        return orderInfoMapper.selectByExample(example);

    }

    @Override
    @Async
    // 处理未完成订单
    public  void execExpiredOrder(OrderInfo orderInfo){
        // 订单信息
        updateOrderStatus(orderInfo.getId(),ProcessStatus.CLOSED);
        // 付款信息
        paymentService.closePayment(orderInfo.getId());
    }


    /**
     *  根据orderId 将orderInfo 变为json 字符串
     * @param orderId
     * @return
     */
    private String initWareOrder(String orderId) {
        // 根据orderId 查询orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);
        // 将orderInfo 中有用的信息保存到map 中！
        Map map = initWareOrder(orderInfo);
        // 将map 转换为json  字符串！
        return JSON.toJSONString(map);

    }

    /**
     *
     * @param orderInfo
     * @return
     */
    public Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        // 给map 的key 赋值！
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody","测试用例");
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");

        map.put("wareId",orderInfo.getWareId()); // 仓库Id
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        // 创建一个集合来存储map
        ArrayList<Map> arrayList = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> orderDetailMap = new HashMap<>();
            orderDetailMap.put("skuId",orderDetail.getSkuId());
            orderDetailMap.put("skuNum",orderDetail.getSkuNum());
            orderDetailMap.put("skuName",orderDetail.getSkuName());
            arrayList.add(orderDetailMap);
        }
        map.put("details",arrayList);

        return map;
    }
    @Override
    public List<OrderInfo> splitOrder(String orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        /*
            1.  获取原始订单
            2.  将wareSkuMap 转换为我们能操作的对象
            3.  创建新的子订单
            4.  给子订单赋值，并保存到数据库
            5.  将子订单添加到集合中
            6.  更新原始订单状态！

         */
        OrderInfo orderInfoOrigin  = getOrderInfo(orderId);
        // wareSkuMap [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        if (maps!=null){
            // 循环遍历集合
            for (Map map : maps) {
                // 获取仓库Id
                String wareId = (String) map.get("wareId");
                // 获取商品Id
                List<String> skuIds = (List<String>) map.get("skuIds");
                OrderInfo subOrderInfo  = new OrderInfo();
                // 属性拷贝
                BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
                // id 必须变为null
                subOrderInfo.setId(null);
                subOrderInfo.setWareId(wareId);
                subOrderInfo.setParentOrderId(orderId);

                // 价格： 获取到原始订单的明细
                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();

                // 声明一个新的子订单明细集合
                ArrayList<OrderDetail> subOrderDetailArrayList = new ArrayList<>();
                // 原始的订单明细商品Id
                for (OrderDetail orderDetail : orderDetailList) {
                    // 仓库对应的商品Id
                    for (String skuId : skuIds) {
                        if (skuId.equals(orderDetail.getSkuId())){
                            orderDetail.setId(null);
                            subOrderDetailArrayList.add(orderDetail);
                        }
                    }
                }
                // 将新的子订单集合放入子订单中
                subOrderInfo.setOrderDetailList(subOrderDetailArrayList);

                // 计算价格：
                subOrderInfo.sumTotalAmount();

                // 保存到数据库
                saveOrder(subOrderInfo);

                // 将新的子订单添加到集合中
                subOrderInfoList.add(subOrderInfo);
            }
        }
        updateOrderStatus(orderId,ProcessStatus.SPLIT);
        return subOrderInfoList;
    }


}
