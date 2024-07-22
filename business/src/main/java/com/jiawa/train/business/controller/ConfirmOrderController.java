package com.jiawa.train.business.controller;

import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.business.service.ConfirmOrderService;
import com.jiawa.train.resp.CommonResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/confirm-order")
public class ConfirmOrderController<businessLoginReq> {
    @Resource
    private ConfirmOrderService confirmOrderService;



    /**
     * 保存
     * @param businessRegisterReq
     * @return
     */
    @PostMapping("/do")
    public CommonResp<Object> doConfirm(@Valid @RequestBody ConfirmOrderDoReq businessRegisterReq) {
        confirmOrderService.doConfirm(businessRegisterReq);
        return new CommonResp<>();
    }



}
