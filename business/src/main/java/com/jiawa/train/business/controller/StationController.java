package com.jiawa.train.business.controller;

import com.jiawa.train.business.req.StationQueryReq;
import com.jiawa.train.business.resp.StationQueryResp;
import com.jiawa.train.business.service.StationService;
import com.jiawa.train.resp.CommonResp;
import com.jiawa.train.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/station")
public class StationController<businessLoginReq> {
    @Resource
    private StationService stationService;

    /**
     * 测试
     *
     * @return
     */
    @GetMapping("/hello")
    public CommonResp hello() {
//        System.out.println("hello world!");;
        return new CommonResp("hello world!");
    }


    /**
     * 查询车站列表
     * @param req
     * @return
     */
    @GetMapping("/query-list")
    public CommonResp<PageResp<StationQueryResp>> queryList(@Valid StationQueryReq req) {
        PageResp<StationQueryResp> stationQueryRespPageResp = stationService.queryList(req);
        return new CommonResp<>(stationQueryRespPageResp);
    }

    /**
     * 查询所有车站
     * @return
     */
    @GetMapping("/query-all")
    public CommonResp<List<StationQueryResp>> queryAll() {
        List<StationQueryResp> stationQueryResps = stationService.queryAll();
        return new CommonResp<List<StationQueryResp>>(stationQueryResps);
    }

}
