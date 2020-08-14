package com.company.gmall.service;

import com.company.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {

    void  addToCart(String skuId,String userId,Integer skuNum);

    // 缓存中没有数据，则从 数据库中加载
    List<CartInfo> loadCartCache(String userId);
    // 查询购物车集合列表
    List<CartInfo> getCartList(String userId);

    List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId);

    void checkCart(String skuId, String isChecked, String userId);

    List<CartInfo> getCartCheckedList(String userId);


    void del(String skuId, String userId);
}
