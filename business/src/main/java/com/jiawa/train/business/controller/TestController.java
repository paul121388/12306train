package com.jiawa.train.business.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @SentinelResource("hello2")
    @GetMapping("/hello2")
    public String hello() throws InterruptedException {
        Thread.sleep(500);
        return "Hello World! Business";
    }

    @SentinelResource("hello1")
    @GetMapping("/hello1")
    public String hello1() {
        return "Hello World! Business";
    }
}
