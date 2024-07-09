package com.jiawa.train.member.controller;

import com.jiawa.train.CommonResp.CommonResp;
import com.jiawa.train.member.req.PassengerSaveReq;
import com.jiawa.train.member.service.PassengerService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
     *
     * @param memberRegisterReq
     * @return
     */
    @PostMapping("/register")
    public CommonResp<Object> save(@Valid @RequestBody PassengerSaveReq memberRegisterReq) {
        passengerService.save(memberRegisterReq);
        return new CommonResp<>();
    }


}
