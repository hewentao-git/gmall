package com.company.gmall.manage.mapper;

import com.company.gmall.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {

    List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu(String spuId);
}
