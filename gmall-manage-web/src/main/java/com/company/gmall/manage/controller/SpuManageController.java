package com.company.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.company.gmall.bean.SpuInfo;
import com.company.gmall.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class SpuManageController {

    @Reference
    private ManageService manageService;

    @RequestMapping("spuList")
    public List<SpuInfo> getSpuList(SpuInfo spuInfo){
        return manageService.getSpuInfoList(spuInfo);
    }

    @RequestMapping("spuImageList")
    public List<SpuInfo> getSpuImageList(String spuId){
        return manageService.getSpuImageList(spuId);
    }
}
