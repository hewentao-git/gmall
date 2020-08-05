package com.company.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.company.gmall.bean.SkuInfo;
import com.company.gmall.bean.SkuLsInfo;
import com.company.gmall.bean.SpuImage;
import com.company.gmall.bean.SpuSaleAttr;
import com.company.gmall.service.ListService;
import com.company.gmall.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class SkuManageController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    @RequestMapping("spuImageList")
    public List<SpuImage> getSpuImageList(SpuImage spuImage) {
        return manageService.getSpuImageList(spuImage);
    }

    @RequestMapping("spuSaleAttrList")
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        return manageService.getSpuSaleAttrList(spuId);
    }

    @RequestMapping("saveSkuInfo")
    public void saveSkuInfo(@RequestBody SkuInfo skuInfo) {

        if (skuInfo != null) {
            manageService.saveSkuInfo(skuInfo);
        }
    }

    //上传一个商品
    @RequestMapping("onSale")
    public void onSale(String skuId){
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        //属性拷贝
        BeanUtils.copyProperties(skuInfo,skuLsInfo);
        listService.saveSkuLsInfo(skuLsInfo);
    }
}
