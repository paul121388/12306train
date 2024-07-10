package com.jiawa.train.context;

import com.jiawa.train.resp.MemberLoginResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginMemberContext {
    private static final Logger LOG = LoggerFactory.getLogger(LoginMemberContext.class);
    private static ThreadLocal<MemberLoginResp> member = new ThreadLocal<>();

    public static MemberLoginResp getMember() {
        return member.get();
    }

    public static void setMember(MemberLoginResp member) {
        LoginMemberContext.member.set(member);
    }

    public static Long getMemberId() {
        try {
            return member.get().getId();
        } catch (Exception e) {
            LOG.error("获取用户ID失败", e);
            throw e;
        }
    }
}
