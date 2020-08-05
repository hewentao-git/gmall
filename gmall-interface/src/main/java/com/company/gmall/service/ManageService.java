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

    List<SpuImage> getSpuImageList(SpuImage spuImage);

    // 查询基本销售属性表
    List<BaseSaleAttr> getBaseSaleAttrList();

    void saveSpuInfo(SpuInfo spuInfo);

    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    void saveSkuInfo(SkuInfo skuInfo);

    SkuInfo getSkuInfo(String skuId);

    /**
     * 根据skuid spuid 查询销售属性值集合
     * @param skuInfo
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpuId(String spuId);

    List<BaseAttrInfo> getAttrList(List<String> attrValueIdList);
}
