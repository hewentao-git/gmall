package com.company.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.company.gmall.bean.CartInfo;
import com.company.gmall.bean.SkuInfo;
import com.company.gmall.config.LoginRequire;
import com.company.gmall.service.CartService;
import com.company.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class CartController {

    @Reference
    private CartService cartService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    @Reference
    private ManageService manageService;

    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response, Model model) {
        //获取商品
        String skuNum = request.getParameter("skuNum");
        String skuId = request.getParameter("skuId");
        //获取用户Id
        String userId = (String) request.getAttribute("userId");

        if (userId != null) {
            //登录添加购物车
            cartService.addToCart(skuId, userId, Integer.parseInt(skuNum));
        } else {
            //未登录添加购物车
            // 没有登录放到cookie中
            cartCookieHandler.addToCart(request, response, skuId, userId, Integer.parseInt(skuNum));

        }

        // 取得sku信息对象
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        model.addAttribute("skuInfo", skuInfo);
        model.addAttribute("skuNum", skuNum);
        return "success";
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response, Model model) {
        // 判断用户是否登录，登录了从redis中，redis中没有，从数据库中取
        // 没有登录，从cookie中取得
        String userId = (String) request.getAttribute("userId");
        if (userId != null) {
            // 从cookie中查找购物车
            List<CartInfo> cartListFromCookie = cartCookieHandler.getCartList(request);
            List<CartInfo> cartList = null;
            if (cartListFromCookie != null && cartListFromCookie.size() > 0) {
                // 开始合并
                cartList = cartService.mergeToCartList(cartListFromCookie, userId);
                // 删除cookie中的购物车
                cartCookieHandler.deleteCartCookie(request, response);
            } else {
                // 从redis中取得，或者从数据库中
                cartList = cartService.getCartList(userId);
            }

            model.addAttribute("cartList", cartList);
        } else {
            List<CartInfo> cartList = cartCookieHandler.getCartList(request);
            model.addAttribute("cartList", cartList);
        }
        return "cartList";
    }

    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request, HttpServletResponse response) {

        String skuId = request.getParameter("skuId");
        String isChecked = request.getParameter("isChecked");
        String userId = (String) request.getAttribute("userId");
        if (userId != null) {
            cartService.checkCart(skuId, isChecked, userId);
        } else {
            cartCookieHandler.checkCart(request, response, skuId, isChecked);
        }
    }
    @RequestMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request,HttpServletResponse response) {

        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cookieHandlerCartList = cartCookieHandler.getCartList(request);

        if (cookieHandlerCartList != null && cookieHandlerCartList.size() > 0) {

            cartService.mergeToCartList(cookieHandlerCartList, userId);
            cartCookieHandler.deleteCartCookie(request, response);
        }
        return "redirect://order.gmall.com/trade";
    }
}
