package com.jiawa.train.member.controller;

import com.jiawa.train.CommonResp.CommonResp;
import com.jiawa.train.member.req.MemberRegisterReq;
import com.jiawa.train.member.service.MemberService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member")
public class MemberController {
    @Resource
    private MemberService memberService;

    @GetMapping("/count")
    public CommonResp count() {
        Integer count = memberService.count();
        return new CommonResp(count);
    }

    @PostMapping("/register")
    public CommonResp register(MemberRegisterReq memberRegisterReq) {
        Long id = memberService.register(memberRegisterReq);
        return new CommonResp(id);
    }


}
