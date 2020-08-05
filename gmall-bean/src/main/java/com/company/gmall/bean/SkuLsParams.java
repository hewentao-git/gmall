package com.company.gmall.bean;

import lombok.Data;

import java.io.Serializable;

@Data
public class SkuLsParams implements Serializable {

    private String  keyword;

    private String catalog3Id;

    private String[] valueId;

    private int pageNo=1;

    private int pageSize=20;

}
