package com.example.coupon.merchant.admin.config;

import cn.hutool.core.util.StrUtil;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * XXL-Job 配置类
 */
@Configuration
@ConditionalOnProperty(prefix = "xxl-job", name = "enable", havingValue = "true", matchIfMissing = true) // 如果xxl-job.enable的值为true或者没有配置，则启用该配置类
public class XXLJobConfiguration {

    @Value("http://localhost:8088/xxl-job-admin")
    private String adminAddresses;

    @Value("default_token")
    private String accessToken;

    @Value("coupon-merchant-admin")
    private String applicationName;

    @Value("127.0.0.1")
    private String ip;

    @Value("19999")
    private int port;

    @Value("${xxl-job.executor.log-path:}")
    private String logPath;

    @Value("30")
    private int logRetentionDays;


    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(applicationName);
        xxlJobSpringExecutor.setIp(ip);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setAccessToken(StrUtil.isNotEmpty(accessToken) ? accessToken : null);
        xxlJobSpringExecutor.setLogPath(StrUtil.isNotEmpty(logPath) ? logPath : Paths.get("").toAbsolutePath().getParent() + "/logs");
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);
        return xxlJobSpringExecutor;
    }







}
