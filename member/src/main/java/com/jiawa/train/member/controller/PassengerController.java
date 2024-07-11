package com.jiawa.train.member.controller;

import com.jiawa.train.context.LoginMemberContext;
import com.jiawa.train.member.req.PassengerQueryReq;
import com.jiawa.train.member.resp.PassengerQueryResp;
import com.jiawa.train.resp.CommonResp;
import com.jiawa.train.member.req.PassengerSaveReq;
import com.jiawa.train.member.service.PassengerService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/passenger")
public class PassengerController<memberLoginReq> {
    @Resource
    private PassengerService passengerService;

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
     * @param memberRegisterReq
     * @return
     */
    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody PassengerSaveReq memberRegisterReq) {
        passengerService.save(memberRegisterReq);
        return new CommonResp<>();
    }

    /**
     * 查询乘客列表
     * @param req
     * @return
     */
    @GetMapping("/query-list")
    public CommonResp<List<PassengerQueryResp>> queryList(PassengerQueryReq req) {
        req.setMemberId(LoginMemberContext.getMemberId());
        List<PassengerQueryResp> passengerQueryRespList = passengerService.queryList(req);
        return new CommonResp<>(passengerQueryRespList);
    }


}
