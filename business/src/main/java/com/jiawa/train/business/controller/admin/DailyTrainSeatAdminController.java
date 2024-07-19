package com.jiawa.train.business.controller.admin;

import com.jiawa.train.business.req.DailyTrainSeatQueryReq;
import com.jiawa.train.business.req.DailyTrainSeatSaveReq;
import com.jiawa.train.business.resp.DailyTrainSeatQueryResp;
import com.jiawa.train.business.service.DailyTrainSeatService;
import com.jiawa.train.resp.CommonResp;
import com.jiawa.train.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/daily-train-seat")
public class DailyTrainSeatAdminController<businessLoginReq> {
    @Resource
    private DailyTrainSeatService dailyTrainSeatService;

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
    public CommonResp<Object> save(@Valid @RequestBody DailyTrainSeatSaveReq businessRegisterReq) {
        dailyTrainSeatService.save(businessRegisterReq);
        return new CommonResp<>();
    }

    /**
     * 查询乘客列表
     * @param req
     * @return
     */
    @GetMapping("/query-list")
    public CommonResp<PageResp<DailyTrainSeatQueryResp>> queryList(@Valid DailyTrainSeatQueryReq req) {
        PageResp<DailyTrainSeatQueryResp> dailyTrainSeatQueryRespPageResp = dailyTrainSeatService.queryList(req);
        return new CommonResp<>(dailyTrainSeatQueryRespPageResp);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResp delete(@PathVariable Long id) {
        dailyTrainSeatService.delete(id);
        return new CommonResp();
    }

}
