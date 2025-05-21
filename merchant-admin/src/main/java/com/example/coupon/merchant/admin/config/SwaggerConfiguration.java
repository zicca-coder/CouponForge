package com.example.coupon.merchant.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

/**
 * 设置文档 API Swagger 配置信息
 */
@Slf4j
@Configuration
public class SwaggerConfiguration implements ApplicationRunner {

    @Value("10010")
    private String serverPort;
    @Value("${server.servlet.context-path}")
    private String contextPath;


    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CouponForge-商家后台管理系统")
                        .description("创建优惠券、店家查看以及管理优惠券、创建优惠券发放批次等")
                        .version("v1.0.0")
                        .contact(new Contact().name("zicca").email("ziq@zjut.edu.cn"))
                        .license(new License().name("Example").url("")));
    }


    /*
     * @Description: 打印日志信息，方便点击连接跳转
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("API Document: http://localhost:{}{}/doc.html", serverPort, contextPath);
    }
}
