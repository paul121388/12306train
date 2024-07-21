package com.jiawa.train.batch.controller;

import com.jiawa.train.batch.feign.BusinessFeign;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    private static final Logger LOG = LoggerFactory.getLogger(TestController.class);
    @Resource
    BusinessFeign businessFeign;

    @GetMapping("/hello")
    public String test() {
        String temp = businessFeign.hello();
        LOG.info("test: {}", temp);
        return "Hello World! Batch!";
    }
}
