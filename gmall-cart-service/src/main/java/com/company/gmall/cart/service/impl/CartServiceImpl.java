package com.company.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.company.gmall.bean.CartInfo;
import com.company.gmall.bean.SkuInfo;
import com.company.gmall.cart.constant.CartConst;
import com.company.gmall.cart.mapper.CartInfoMapper;
import com.company.gmall.config.RedisUtil;
import com.company.gmall.service.CartService;
import com.company.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 登录时添加购物车
     *
     * @param skuId
     * @param userId
     * @param skuNum
     */
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
//        先查询一下购物车中是否有相同的商品，如果有则数量相加
//        如果没有，直接添加到数据库!
//        更新缓存!

        //先通过用户商品查询
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setUserId(userId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfo);

        if (cartInfoExist != null) {
            //有相同商品
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());

            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
        } else {
            //没有相同商品

            SkuInfo skuInfo = manageService.getSkuInfo(skuId);

            CartInfo cartInfo1 = new CartInfo();
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setSkuId(skuId);
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuNum(skuNum);

            //保存数据库
            cartInfoMapper.insertSelective(cartInfo1);

            cartInfoExist = cartInfo1;
        }

        //更新缓存
        // 构建key user:userid:cart
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

        Jedis jedis = redisUtil.getJedis();
        // 将对象序列化
        String cartJson = JSON.toJSONString(cartInfoExist);
        jedis.hset(userCartKey, skuId, cartJson);
        // 设置购物车过期时间
        String userInfoKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USERINFOKEY_SUFFIX;
        //获取用户过期时间
        Long ttl = jedis.ttl(userInfoKey);
        //设置购物车过期时间    让购物车和用户一起过期
        jedis.expire(userCartKey, ttl.intValue());
        jedis.close();

    }

    public List<CartInfo> getCartList(String userId) {
        // 从redis中取得，
        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        List<String> cartJsons = jedis.hvals(userCartKey);

        if (cartJsons != null && cartJsons.size() > 0) {
            List<CartInfo> cartInfoList = new ArrayList<>();
            for (String cartJson : cartJsons) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            // 排序
            cartInfoList.sort(Comparator.comparing(CartInfo::getId));

            return cartInfoList;
        } else {
            // 从数据库中查询，其中cart_price 可能是旧值，所以需要关联sku_info 表信息。
            List<CartInfo> cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }

    public List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId) {

        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCurPrice(userId);

        // 循环开始匹配
        for (CartInfo cartInfoCk : cartListFromCookie) {
            boolean isMatch = false;
            for (CartInfo cartInfoDB : cartInfoListDB) {
                if (cartInfoDB.getSkuId().equals(cartInfoCk.getSkuId())) {
                    cartInfoDB.setSkuNum(cartInfoCk.getSkuNum() + cartInfoDB.getSkuNum());
                    cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                    isMatch = true;
                }
            }
            // 数据库中没有购物车，则直接将cookie中购物车添加到数据库
            if (!isMatch) {
                cartInfoCk.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCk);
            }
        }
        // 从新在数据库中查询并返回数据
        List<CartInfo> cartInfoList = loadCartCache(userId);

        for (CartInfo cartInfo : cartInfoList) {
            for (CartInfo info : cartListFromCookie) {
                if (cartInfo.getSkuId().equals(info.getSkuId())) {
                    // 只有被勾选的才会进行更改
                    if ("1".equals(info.getIsChecked())) {
                        cartInfo.setIsChecked(info.getIsChecked());
                        // 更新redis中的isChecked
                        checkCart(cartInfo.getSkuId(), info.getIsChecked(), userId);
                    }
                }
            }
        }

        return cartInfoList;
    }

    public void checkCart(String skuId, String isChecked, String userId) {
        // 更新购物车中的isChecked标志
        Jedis jedis = redisUtil.getJedis();
        // 取得购物车中的信息
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        String cartJson = jedis.hget(userCartKey, skuId);
        // 将cartJson 转换成对象
        CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
        cartInfo.setIsChecked(isChecked);
        String cartCheckdJson = JSON.toJSONString(cartInfo);
        jedis.hset(userCartKey, skuId, cartCheckdJson);
        // 新增到已选中购物车
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        if ("1".equals(isChecked)) {
            jedis.hset(userCheckedKey, skuId, cartCheckdJson);
        } else {
            jedis.hdel(userCheckedKey, skuId);
        }
        jedis.close();
    }

    // 得到选中购物车列表
    public List<CartInfo> getCartCheckedList(String userId) {
        // 获得redis中的key
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        List<String> cartCheckedList = jedis.hvals(userCheckedKey);
        List<CartInfo> newCartList = new ArrayList<>();
        for (String cartJson : cartCheckedList) {
            CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
            newCartList.add(cartInfo);
        }
        return newCartList;
    }

    @Override
    public void del(String skuId, String userId) {
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setUserId(userId);
        cartInfoMapper.delete(cartInfo);
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        jedis.hdel(userCartKey,skuId);
        jedis.del(userCheckedKey);
        jedis.close();
    }


    /**
     * 购物车查询，在数据库中查找
     *
     * @param userId
     * @return
     */
    public List<CartInfo> loadCartCache(String userId) {

        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfoList == null && cartInfoList.size() == 0) {
            return null;
        }
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

        Jedis jedis = redisUtil.getJedis();
        Map<String, String> map = new HashMap<>(cartInfoList.size());
        for (CartInfo cartInfo : cartInfoList) {
            String cartJson = JSON.toJSONString(cartInfo);
            // key 都是同一个，值会产生重复覆盖！
            map.put(cartInfo.getSkuId(), cartJson);
        }
        // 将java list - redis hash
        jedis.hmset(userCartKey, map);
        jedis.close();
        return cartInfoList;
    }

}
