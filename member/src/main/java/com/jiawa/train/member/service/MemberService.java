package com.jiawa.train.member.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.jiawa.train.exception.BusinessException;
import com.jiawa.train.exception.BusinessExceptionEnum;
import com.jiawa.train.member.domain.Member;
import com.jiawa.train.member.domain.MemberExample;
import com.jiawa.train.member.mapper.MemberMapper;
import com.jiawa.train.member.req.MemberRegisterReq;
import com.jiawa.train.member.req.MemberSendCodeReq;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {
    private static final Logger LOG = LoggerFactory.getLogger(MemberService.class);
    @Resource
    private MemberMapper memberMapper;

    /**
     * 获取会员数量
     * @return
     */
    public int count() {
        return Math.toIntExact(memberMapper.countByExample(null));
    }

    /**
     * 注册会员
     * @param memberRegisterReq
     * @return
     */
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

    public void sendCode(MemberSendCodeReq memberSendCodeReq) {
        // 0. 获取手机号
        String mobile = memberSendCodeReq.getMobile();
        // 1. 判断手机号是否已经注册过
        MemberExample memberExample = new MemberExample();
        memberExample.createCriteria().andMobileEqualTo(mobile);

        List<Member> members = memberMapper.selectByExample(memberExample);
        if(CollUtil.isEmpty(members)){
            LOG.info("手机号未注册过,插入一条手机号");
            Member member = new Member();
            member.setId(SnowUtil.getSnowflakeNextId());
            member.setMobile(mobile);

            memberMapper.insert(member);
        }
        else {
            LOG.info("手机号已注册,不插入手机号");
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_EXIST);
        }

        // 生成验证码
        String code = RandomUtil.randomString(4);
        LOG.info("手机号:{},验证码:{}", mobile, code);
    }
}
