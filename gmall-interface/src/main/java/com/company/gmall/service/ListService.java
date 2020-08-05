package com.company.gmall.service;

import com.company.gmall.bean.SkuLsInfo;
import com.company.gmall.bean.SkuLsParams;
import com.company.gmall.bean.SkuLsResult;

public interface ListService {

    void saveSkuLsInfo(SkuLsInfo skuLsInfo);

    SkuLsResult search(SkuLsParams skuLsParams);
}
