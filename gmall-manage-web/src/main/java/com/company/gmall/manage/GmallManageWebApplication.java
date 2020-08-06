package com.company.gmall.manage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.company.gmall")
public class GmallManageWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallManageWebApplication.class, args);
    }

}
