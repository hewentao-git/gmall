package com.company.gmall.manage.mapper;

import com.company.gmall.bean.BaseAttrInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    List<BaseAttrInfo> selectBaseAttrInfoByCatalog3Id(String catalog3Id);
}
