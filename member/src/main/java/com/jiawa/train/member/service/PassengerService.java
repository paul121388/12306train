package com.jiawa.train.member.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.context.LoginMemberContext;
import com.jiawa.train.member.domain.Passenger;
import com.jiawa.train.member.domain.PassengerExample;
import com.jiawa.train.member.mapper.PassengerMapper;
import com.jiawa.train.member.req.PassengerQueryReq;
import com.jiawa.train.member.req.PassengerSaveReq;
import com.jiawa.train.member.resp.PassengerQueryResp;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PassengerService {
    private static final Logger LOG = LoggerFactory.getLogger(PassengerService.class);

    @Resource
    private PassengerMapper passengerMapper;

    /**
     * 乘客保存
     * @param req
     */
    public void save(PassengerSaveReq req){
        DateTime now = DateTime.now();
        Passenger passenger = BeanUtil.copyProperties(req, Passenger.class);
        passenger.setMemberId(LoginMemberContext.getMemberId());
        passenger.setId(SnowUtil.getSnowflakeNextId());
        passenger.setCreateTime(now);
        passenger.setUpdateTime(now);
        passengerMapper.insert(passenger);
    }

    /**
     * 乘客查询 1.控制端查询所有乘客  2.member查询当前乘客
     * @param req
     */
    public PageResp<PassengerQueryResp> queryList(PassengerQueryReq req){
        PassengerExample passengerExample = new PassengerExample();
        PassengerExample.Criteria criteria = passengerExample.createCriteria();
        if(ObjectUtil.isNotNull(LoginMemberContext.getMemberId())){
            // 为了让后续控制台调用方法时，没有会员ID（因为不是会员登录）也能使用这个方法，这里的memberID应该在controller赋值
//            criteria.andMemberIdEqualTo(LoginMemberContext.getMemberId());
            criteria.andMemberIdEqualTo(req.getMemberId());
        }
        // 分页查询语句尽量与需要分页查询的sql语句放在一起，因为其只对最近的一条select语句生效
        LOG.info("页数：{}", req.getPage());
        LOG.info("每页大小：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<Passenger> passengerList = passengerMapper.selectByExample(passengerExample);

        // 获取总数
        PageInfo<Passenger> passengerPageInfo = new PageInfo<>(passengerList);
        LOG.info("总行数：{}", passengerPageInfo.getTotal());
        LOG.info("总页数：{}", passengerPageInfo.getPages());

        List<PassengerQueryResp> passengerQueryRespList = BeanUtil.copyToList(passengerList, PassengerQueryResp.class);

        PageResp<PassengerQueryResp> objectPageResp = new PageResp<>();
        objectPageResp.setTotal(passengerPageInfo.getTotal());
        objectPageResp.setList(passengerQueryRespList);
        return objectPageResp;
    }
}
