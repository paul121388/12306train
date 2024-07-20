package com.jiawa.train.batch.feign;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "business", url = "http://127.0.0.1:8002/business")
public interface BusinessFeign {
}
