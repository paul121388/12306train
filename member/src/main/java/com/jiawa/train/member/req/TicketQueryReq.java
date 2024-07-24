package com.jiawa.train.member.req;

import com.jiawa.train.req.PageReq;

public class TicketQueryReq extends PageReq {
private Long memberId;

    @Override
    public String toString() {
        return "TicketQueryReq{" +
                "memberId=" + memberId +
                '}';
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

}
