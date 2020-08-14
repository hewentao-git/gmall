package com.company.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.company.gmall.bean.CartInfo;
import com.company.gmall.bean.SkuInfo;
import com.company.gmall.config.CookieUtil;
import com.company.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class CartCookieHandler {

    // 定义购物车名称
    private String cookieCartName = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE = 7 * 24 * 3600;

    @Reference
    private ManageService manageService;

    /**
     * 添加购物车
     *
     * @param request
     * @param response
     * @param skuId
     * @param userId
     * @param skuNum
     */
    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, int skuNum) {
        /**
         * 查看购物车是否有次商品
         * 有：数量相加
         * 无：添加
         */
        //从cookie中获取购物车
        String cartJson = CookieUtil.getCookieValue(request, cookieCartName, true);
        List<CartInfo> cartInfoList = new ArrayList<>();
        boolean ifExist = false;
        if (StringUtils.isNotEmpty(cartJson)) {
            cartInfoList = JSON.parseArray(cartJson, CartInfo.class);

            for (CartInfo cartInfo : cartInfoList) {

                if (cartInfo.getSkuId().equals(skuId)) {
                    cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);
                    //实时价格初始化
                    cartInfo.setSkuPrice(cartInfo.getCartPrice());
                    ifExist = true;
                }
            }
        }
        //购物车中不存在
        if (!ifExist){
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo = new CartInfo();


            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuId(skuId);
            cartInfo.setSkuNum(skuNum);
            cartInfo.setSkuId(skuId);
            cartInfoList.add(cartInfo);

        }

        //更新购物车
        String newCartJson = JSON.toJSONString(cartInfoList);
        CookieUtil.setCookie(request,response,cookieCartName,newCartJson,COOKIE_CART_MAXAGE,true);

    }

    /**
     * 查询cookie 中购物车列表
     * @param request
     * @return
     */
    public List<CartInfo> getCartList(HttpServletRequest request){
        String cartJson = CookieUtil.getCookieValue(request, cookieCartName, true);
        if (StringUtils.isNotEmpty(cartJson)) {
            List<CartInfo> cartInfoList = JSON.parseArray(cartJson, CartInfo.class);
            return cartInfoList;
        }
        return null;
    }

    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request,response,cookieCartName);
    }

    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {
        //  取出购物车中的商品
        List<CartInfo> cartList = getCartList(request);
        // 循环比较
        for (CartInfo cartInfo : cartList) {
            if (cartInfo.getSkuId().equals(skuId)){
                cartInfo.setIsChecked(isChecked);
            }
        }
        // 保存到cookie
        String newCartJson = JSON.toJSONString(cartList);
        CookieUtil.setCookie(request,response,cookieCartName,newCartJson,COOKIE_CART_MAXAGE,true);
    }

}
