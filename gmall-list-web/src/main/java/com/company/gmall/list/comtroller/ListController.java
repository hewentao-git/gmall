package com.company.gmall.list.comtroller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.company.gmall.bean.SkuLsParams;
import com.company.gmall.bean.SkuLsResult;
import com.company.gmall.service.ListService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ListController {

    @Reference
    private ListService listService;

    @RequestMapping("list.html")
    public String getList(SkuLsParams skuLsParams){
        SkuLsResult search = listService.search(skuLsParams);
        return JSON.toJSONString(search);
    }

}
