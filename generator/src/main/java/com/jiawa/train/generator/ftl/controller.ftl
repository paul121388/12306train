package com.jiawa.train.${module}.controller;

import com.jiawa.train.context.LoginMemberContext;
import com.jiawa.train.${module}.req.${Domain}QueryReq;
import com.jiawa.train.${module}.req.${Domain}SaveReq;
import com.jiawa.train.${module}.resp.${Domain}QueryResp;
import com.jiawa.train.${module}.service.${Domain}Service;
import com.jiawa.train.resp.CommonResp;
import com.jiawa.train.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/${do_main}")
public class ${Domain}Controller<${module}LoginReq> {
    @Resource
    private ${Domain}Service ${domain}Service;

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
     * @param ${module}RegisterReq
     * @return
     */
    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody ${Domain}SaveReq ${module}RegisterReq) {
        ${domain}Service.save(${module}RegisterReq);
        return new CommonResp<>();
    }

    /**
     * 查询乘客列表
     * @param req
     * @return
     */
    @GetMapping("/query-list")
    public CommonResp<PageResp<${Domain}QueryResp>> queryList(@Valid ${Domain}QueryReq req) {
        req.setMemberId(LoginMemberContext.getMemberId());
        PageResp<${Domain}QueryResp> ${domain}QueryRespPageResp = ${domain}Service.queryList(req);
        return new CommonResp<>(${domain}QueryRespPageResp);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResp delete(@PathVariable Long id) {
        ${domain}Service.delete(id);
        return new CommonResp();
    }

}
