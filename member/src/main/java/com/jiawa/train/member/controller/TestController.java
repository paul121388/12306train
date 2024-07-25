package com.jiawa.train.member.controller;

//import jakarta.annotation.ource;
import org.springframework.core.env.Environment;
//import org.apache.ibatis.mapping.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
public class TestController {
    @Value("${test.nacos}")
    private String test;
    @Autowired
    Environment environment;

    @GetMapping("/hello")
    public String test() {
        String property = environment.getProperty("local.server.port");
        return String.format("Hello Nacos:%s，端口:%s", test, property);
    }
}
