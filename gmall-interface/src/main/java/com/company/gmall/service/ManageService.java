package com.company.gmall.service;

import com.company.gmall.bean.*;

import java.util.List;

public interface ManageService {

    /**
     *查询一级分类数据
     * select* from baseCatalog1
     * @return
     */
    List<BaseCatalog1> getCatalog1();

    /**
     *很据一级分类Id查询二级分类数据
     * select* from baseCatalog2 where catalog1Id =?
     * @param catalog1Id
     * @return
     */
    List<BaseCatalog2> getCatalog2(String catalog1Id);
    /**
     *很据一级分类Id查询二级分类数据
     * select* from baseCatalog3 where catalog2Id =?
     * @param catalog2Id
     * @return
     */
    List<BaseCatalog3> getCatalog3(String catalog2Id);
    /**
     *很据三级分类Id查询
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> getAttrList(String catalog3Id);

    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    List<BaseAttrValue> getAttrValueList(String attrId);

    BaseAttrInfo getAttrInfo(String attrId);

    List<SpuInfo> getSpuInfoList(SpuInfo spuInfo);

    List<SpuInfo> getSpuImageList(String spuId);
}
