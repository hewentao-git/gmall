package com.company.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.company.gmall.bean.UserAddress;
import com.company.gmall.bean.UserInfo;
import com.company.gmall.service.UserService;
import com.company.gmall.user.mapper.UserAddressMapper;
import com.company.gmall.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;


import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserInfoMapper userInfoMapper;
    @Autowired
    UserAddressMapper userAddressMapper;

    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserAddress> getUserAddressList(String userId) {

        UserAddress userAddress = new UserAddress();
        userAddress.setId(userId);
        return userAddressMapper.select(userAddress);
    }
}
