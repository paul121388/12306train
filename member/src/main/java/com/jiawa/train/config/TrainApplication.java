package com.jiawa.train.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
@ComponentScan(basePackages = {"com.jiawa"})
public class TrainApplication {
    private static final Logger LOG = LoggerFactory.getLogger(TrainApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(TrainApplication.class, args);
        ConfigurableEnvironment environment = run.getEnvironment();
        LOG.info("启动成功！！");
        LOG.info("地址:\thttp://127.0.0.1:{}", environment.getProperty("server.port"));
    }

}
