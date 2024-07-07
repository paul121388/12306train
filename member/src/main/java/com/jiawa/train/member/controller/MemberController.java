package com.jiawa.train.member.controller;

import com.jiawa.train.CommonResp.CommonResp;
import com.jiawa.train.member.req.MemberLoginReq;
import com.jiawa.train.member.req.MemberRegisterReq;
import com.jiawa.train.member.req.MemberSendCodeReq;
import com.jiawa.train.member.resp.MemberLoginResp;
import com.jiawa.train.member.service.MemberService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member")
public class MemberController<memberLoginReq> {
    @Resource
    private MemberService memberService;

    @GetMapping("/count")
    public CommonResp<Integer> count() {
        Integer count = memberService.count();
        return new CommonResp(count);
    }

    @PostMapping("/register")
    public CommonResp<Long> register(@Valid MemberRegisterReq memberRegisterReq) {
        Long id = memberService.register(memberRegisterReq);
        return new CommonResp(id);
    }

    /**
     * 发送验证码
     * @param memberSendCodeReq
     * @return
     */
    @PostMapping("/send-code")
    public CommonResp register(@Valid MemberSendCodeReq memberSendCodeReq) {
        memberService.sendCode(memberSendCodeReq);
        return new CommonResp();
    }

    @PostMapping("/login")
    public CommonResp<MemberLoginResp> register(@Valid MemberLoginReq memberLoginReq) {
        MemberLoginResp resp = memberService.login(memberLoginReq);
        return new CommonResp<>(resp);
    }




}
