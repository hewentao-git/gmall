package com.company.gmall.service;

import com.company.gmall.bean.UserAddress;
import com.company.gmall.bean.UserInfo;

import java.util.List;

public interface UserService {

    /**
     * 查询所有数据
     * @return
     */
    List<UserInfo> findAll();

    List<UserAddress> getUserAddressList(String userId);
}
