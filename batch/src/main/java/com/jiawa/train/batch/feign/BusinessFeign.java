package com.jiawa.train.batch.feign;

import com.jiawa.train.resp.CommonResp;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Date;

//@FeignClient(name = "business", url = "http://127.0.0.1:8002/business")
@FeignClient(name = "business", fallback = BusinessFeignFallback.class)
public interface BusinessFeign {
    @GetMapping("/business/hello")
    String hello();

    // 调用DailyTrainAdminController的接口
    @GetMapping("/business/admin/daily-train/gen-daily/{date}")
    CommonResp genDaily(@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date date);
}
