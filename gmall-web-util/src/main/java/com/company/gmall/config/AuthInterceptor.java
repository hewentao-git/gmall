package com.company.gmall.config;

import com.alibaba.fastjson.JSON;
import com.company.gmall.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    //用户进去控制器之前
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getParameter("newToken");

        if (!StringUtils.isEmpty(token)) {

            CookieUtil.setCookie(request,response,"token",token,WebConst.COOKIE_MAXAGE,false);
        }else {
            //当用户访问非登录之后的页面，登陆之后，继续访问其他业务模块，URL中没有newtoken ，但是后台可能将token放去cookie中
            token = CookieUtil.getCookieValue(request,"token",false);
        }

        if (!StringUtils.isEmpty(token)){
            //读取token
            Map map = getUserMapByToken(token);
            String nickName = (String) map.get("nickName");
            request.setAttribute("nickName", nickName);
        }

        //在拦截器中获取方法上的注解
//        HandlerMethod handlerMethod = (HandlerMethod) handler;
//        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        Method[] methods = handler.getClass().getMethods();

        for (Method method : methods) {
            LoginRequire annotation = method.getAnnotation(LoginRequire.class);
            if (annotation != null){
                //有注解
                //判断用户是否登录
                //获取服务器的ip
                String salt = request.getHeader("X-forwarded-for");
                //调用verify（）认证
                String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&salt=" + salt);
                if ("success".equals(result)){
                    //登录，认证成功
                    //保存userId
                    Map map = getUserMapByToken(token);
                    String userId = (String) map.get("userId");
                    request.setAttribute("userId", userId);
                    return true;
                }else {
                    //认证失败 methodAnnotation.autoRedirect()=true  必须登录
                    if(annotation.autoRedirect()){
                        String  requestURL = request.getRequestURL().toString();
                        String encodeURL = URLEncoder.encode(requestURL, "UTF-8");
                        response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encodeURL);
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private Map getUserMapByToken(String token) {
        //获取中间部分
        String tokenUserInfo = StringUtils.substringBetween(token, ".");
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] tokenBytes = base64UrlCodec.decode(tokenUserInfo);
        String tokenJson = null;
        try {
            tokenJson = new String(tokenBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Map map = JSON.parseObject(tokenJson, Map.class);
        return map;

    }

}
