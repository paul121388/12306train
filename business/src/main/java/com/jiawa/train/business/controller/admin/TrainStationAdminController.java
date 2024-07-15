package com.jiawa.train.business.controller.admin;

import com.jiawa.train.business.req.TrainStationQueryReq;
import com.jiawa.train.business.req.TrainStationSaveReq;
import com.jiawa.train.business.resp.TrainStationQueryResp;
import com.jiawa.train.business.service.TrainStationService;
import com.jiawa.train.resp.CommonResp;
import com.jiawa.train.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/train-station")
public class TrainStationAdminController<businessLoginReq> {
    @Resource
    private TrainStationService trainStationService;

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
     * 保存
     * @param businessRegisterReq
     * @return
     */
    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody TrainStationSaveReq businessRegisterReq) {
        trainStationService.save(businessRegisterReq);
        return new CommonResp<>();
    }

    /**
     * 查询乘客列表
     * @param req
     * @return
     */
    @GetMapping("/query-list")
    public CommonResp<PageResp<TrainStationQueryResp>> queryList(@Valid TrainStationQueryReq req) {
        PageResp<TrainStationQueryResp> trainStationQueryRespPageResp = trainStationService.queryList(req);
        return new CommonResp<>(trainStationQueryRespPageResp);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResp delete(@PathVariable Long id) {
        trainStationService.delete(id);
        return new CommonResp();
    }

}
