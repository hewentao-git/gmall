package com.company.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.company.gmall.bean.UserAddress;
import com.company.gmall.bean.UserInfo;
import com.company.gmall.config.RedisUtil;
import com.company.gmall.service.UserService;
import com.company.gmall.user.mapper.UserAddressMapper;
import com.company.gmall.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;


import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserAddressMapper userAddressMapper;
    @Autowired
    private RedisUtil redisUtil;

    public String userKey_prefix = "user:";
    public String userinfoKey_suffix = ":info";
    public int userKey_timeOut = 60 * 60 * 24;

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

    @Override
    public UserInfo login(UserInfo userInfo) {
        Jedis jedis = null;
        UserInfo info = null;
        try {
            // select * from userInfo where loginName = ? and passwd=?
        /*
            1.  根据当前的sql 语句 查询是否有当前用户
            2.  将用户信息存储到缓存中！
         */
            // 202cb962ac59075b964b07152d234b70 密码需要进行加密
            String passwd = userInfo.getPasswd();
            // 对密码进行加密
            String newPwd = DigestUtils.md5DigestAsHex(passwd.getBytes());
            // 将加密后的密码赋值给当前对象
            userInfo.setPasswd(newPwd);

            info = userInfoMapper.selectOne(userInfo);

            if (info!=null){
                // 获取Jedis
                jedis = redisUtil.getJedis();
                // 放入redis ,必须起key=user:userId:info
                String userKey = userKey_prefix+info.getId()+userinfoKey_suffix;
                // 哪种数据类型
                jedis.setex(userKey,userKey_timeOut, JSON.toJSONString(info));

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭jedis
            if (jedis != null) {
                jedis.close();
            }
            return info;
        }
    }

    @Override
    public UserInfo verify(String userId) {
        Jedis jedis = null;
        UserInfo userInfo = null;
        try {
            jedis = redisUtil.getJedis();
            String userKey = userKey_prefix + userId + userinfoKey_suffix;
            String userJson = jedis.get(userKey);
            if (!StringUtils.isEmpty(userJson)){
                userInfo = JSON.parseObject(userJson, UserInfo.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
            return userInfo;
        }
    }

    @Override
    public void logout(UserInfo userInfo) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String userKey = userKey_prefix + userInfo.getId() + userinfoKey_suffix;
            if (!StringUtils.isEmpty(userKey)) {
                jedis.del(userKey);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }


}
