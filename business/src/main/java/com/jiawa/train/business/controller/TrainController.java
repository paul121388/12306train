package com.jiawa.train.business.controller;

import com.jiawa.train.business.req.TrainQueryReq;
import com.jiawa.train.business.resp.TrainQueryResp;
import com.jiawa.train.business.service.TrainSeatService;
import com.jiawa.train.business.service.TrainService;
import com.jiawa.train.resp.CommonResp;
import com.jiawa.train.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/train")
public class TrainController<businessLoginReq> {
    @Resource
    private TrainService trainService;

    @Resource
    private TrainSeatService trainSeatService;


    /**
     * 查询火车列表
     * @param req
     * @return
     */
    @GetMapping("/query-list")
    public CommonResp<PageResp<TrainQueryResp>> queryList(@Valid TrainQueryReq req) {
        PageResp<TrainQueryResp> trainQueryRespPageResp = trainService.queryList(req);
        return new CommonResp<>(trainQueryRespPageResp);
    }

    /**
     * 查询所有火车
     * @return
     */
    @GetMapping("/query-all")
    public CommonResp<List<TrainQueryResp>> queryAll() {
        List<TrainQueryResp> trainQueryResps = trainService.queryAll();
        return new CommonResp<>(trainQueryResps);
    }


}
