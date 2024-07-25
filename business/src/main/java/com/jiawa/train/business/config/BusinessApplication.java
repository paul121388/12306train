package com.jiawa.train.business.config;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
@ComponentScan(basePackages = {"com.jiawa"})
@MapperScan("com.jiawa.train.business.mapper")
@EnableFeignClients("com.jiawa.train.business.feign")
@EnableCaching
public class BusinessApplication {
    private static final Logger LOG = LoggerFactory.getLogger(BusinessApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(BusinessApplication.class, args);
        ConfigurableEnvironment environment = run.getEnvironment();
        LOG.info("启动成功！！");
        LOG.info("地址:\thttp://127.0.0.1:{}{}/test", environment.getProperty("server.port"),
                environment.getProperty("server.servlet.context-path"));
    }

}
