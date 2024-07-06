package com.jiawa.train.member.service;

import com.jiawa.train.exception.BusinessException;
import com.jiawa.train.exception.BusinessExceptionEnum;
import com.jiawa.train.member.domain.Member;
import com.jiawa.train.member.domain.MemberExample;
import com.jiawa.train.member.mapper.MemberMapper;
import com.jiawa.train.member.req.MemberRegisterReq;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    @Resource
    private MemberMapper memberMapper;

    public int count() {
        return Math.toIntExact(memberMapper.countByExample(null));
    }

    public Long register(MemberRegisterReq memberRegisterReq) {
        // 0. 获取手机号
        String mobile = memberRegisterReq.getMobile();
        // 1. 判断手机号是否已经注册过
        MemberExample memberExample = new MemberExample();
        memberExample.createCriteria().andMobileEqualTo(mobile);

        List<Member> members = memberMapper.selectByExample(memberExample);
        if(!members.isEmpty()){
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_EXIST);
        }

        // 2. 注册新的手机号
        Member member = new Member();
        member.setId(SnowUtil.getSnowflakeNextId());
        member.setMobile(mobile);

        memberMapper.insert(member);
        return member.getId();
    }
}
