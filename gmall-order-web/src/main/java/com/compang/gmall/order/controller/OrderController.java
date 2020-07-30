package com.compang.gmall.order.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.company.gmall.bean.UserAddress;
import com.company.gmall.service.UserService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class OrderController {

    @Reference
    private UserService userService;

    @RequestMapping("trade")
    public List<UserAddress> trade(String userId){

        return userService.getUserAddressList(userId);
    }
}
