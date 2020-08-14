package com.company.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.company.gmall.bean.SkuImage;
import com.company.gmall.bean.SkuInfo;
import com.company.gmall.bean.SkuSaleAttrValue;
import com.company.gmall.bean.SpuSaleAttr;
import com.company.gmall.config.LoginRequire;
import com.company.gmall.service.ListService;
import com.company.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
public class ItemController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable("skuId") String skuId, Model model) {
        //根据skuid获取数据
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        //查询销售属性，销售属性值集合 spuId skuId
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);
        //获取销售属性值Id
        List<SkuSaleAttrValue> skuSaleAttrValueList = manageService.getSkuSaleAttrValueListBySpuId(skuInfo.getSpuId());
        //把列表变换成 {"118|120":"33","119|122":"34"}
        String valueIdsKey = "";

        Map<String, String> valuesSkuMap = new HashMap<String, String>();

        for (int i = 0; i < skuSaleAttrValueList.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueList.get(i);
            if (valueIdsKey.length() != 0) {
                valueIdsKey +=  "|";
            }
            valueIdsKey += skuSaleAttrValue.getSaleAttrValueId();

            if ((i + 1) == skuSaleAttrValueList.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueList.get(i + 1).getSkuId())) {

                valuesSkuMap.put(valueIdsKey, skuSaleAttrValue.getSkuId());
                valueIdsKey = "";
            }

        }

        //把map变成json串
        String valuesSkuJson = JSON.toJSONString(valuesSkuMap);

        model.addAttribute("skuInfo", skuInfo);
        model.addAttribute("spuSaleAttrList", spuSaleAttrList);
        model.addAttribute("skuSaleAttrValueList", skuSaleAttrValueList);
        model.addAttribute("valuesSkuJson", valuesSkuJson);
        listService.incrHotScore(skuId);  //最终应该由异步方式调用
        return "item";
    }

}
