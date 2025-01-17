package com.jiawa.train.business.controller.admin;

import com.jiawa.train.business.req.TrainQueryReq;
import com.jiawa.train.business.req.TrainSaveReq;
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
@RequestMapping("/admin/train")
public class TrainAdminController<businessLoginReq> {
    @Resource
    private TrainService trainService;

    @Resource
    private TrainSeatService trainSeatService;

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
    public CommonResp<Object> save(@Valid @RequestBody TrainSaveReq businessRegisterReq) {
        trainService.save(businessRegisterReq);
        return new CommonResp<>();
    }

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
     * 根据id删除火车
     * @param id
     * @return
     */
    @DeleteMapping("/delete/{id}")
    public CommonResp delete(@PathVariable Long id) {
        trainService.delete(id);
        return new CommonResp();
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

    /**
     * 根据火车的code，生成座位
     * @param TrainCode
     * @return
     */
    @GetMapping("/gen-seat/{TrainCode}")
    public CommonResp genSeat(@PathVariable String TrainCode) {
        trainSeatService.genTrainSeat(TrainCode);
        return new CommonResp();
    }

}
