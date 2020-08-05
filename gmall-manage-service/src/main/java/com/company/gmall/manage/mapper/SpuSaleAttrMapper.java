package com.company.gmall.manage.mapper;

import com.company.gmall.bean.SpuSaleAttr;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr> {
    /**
     * SpuSaleAttrMapper.xml
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> selectSpuSaleAttrList(String spuId);

    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(@Param("id") String id,@Param("spuId") String spuId);
}
