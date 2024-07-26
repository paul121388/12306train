package com.jiawa.train.member.controller.feign;

import com.jiawa.train.member.service.TicketService;
import com.jiawa.train.req.MemberTicketReq;
import com.jiawa.train.resp.CommonResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feign/ticket")
public class FeignTicketController {
    @Resource
    private TicketService ticketService;

    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody MemberTicketReq req) throws Exception {
        ticketService.save(req);
        return new CommonResp<>();
    }
}
