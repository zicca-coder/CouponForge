package com.example.coupon.merchant.admin;

import com.mzt.logapi.starter.annotation.EnableLogRecord;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.coupon.merchant.admin.dao.mapper")
@EnableLogRecord(tenant = "MerchantAdmin")
public class MerchantAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(MerchantAdminApplication.class, args);
    }

}
