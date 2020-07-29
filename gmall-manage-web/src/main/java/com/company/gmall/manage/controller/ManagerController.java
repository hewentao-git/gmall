package com.company.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.company.gmall.bean.BaseAttrInfo;
import com.company.gmall.bean.BaseCatalog1;
import com.company.gmall.bean.BaseCatalog2;
import com.company.gmall.bean.BaseCatalog3;
import com.company.gmall.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class ManagerController {

    @Reference
    private ManageService manageService;

    @GetMapping("getCatalog1")
    public List<BaseCatalog1> getCatalog1(){
        return manageService.getCatalog1();
    }

    @GetMapping("getCatalog2")
    public List<BaseCatalog2> getCatalog2(String catalog1Id){
        return manageService.getCatalog2(catalog1Id);
    }

    @GetMapping("getCatalog3")
    public List<BaseCatalog3> getCatalog3(String catalog2Id){
        return manageService.getCatalog3(catalog2Id);
    }

    @GetMapping("attrInfoList")
    public List<BaseAttrInfo> attrInfoList(String catalog3Id){
        return manageService.getAttrList(catalog3Id);
    }

}
