package com.jiawa.train.member.req;

import com.jiawa.train.req.PageReq;

/**
 * 查询乘客信息请求
 */
public class PassengerQueryReq extends PageReq {
    // 通过threadlocal获取
    private Long memberId;

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    @Override
    public String toString() {
        return "PassengerQueryReq{" +
                "memberId=" + memberId +
                '}';
    }
}