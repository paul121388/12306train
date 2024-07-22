package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.ConfirmOrder;
import com.jiawa.train.business.domain.ConfirmOrderExample;
import com.jiawa.train.business.enums.ConfirmOrderStatusEnum;
import com.jiawa.train.business.mapper.ConfirmOrderMapper;
import com.jiawa.train.business.req.ConfirmOrderQueryReq;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.business.resp.ConfirmOrderQueryResp;
import com.jiawa.train.context.LoginMemberContext;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfirmOrderService {
    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;

    /**
     * 1.新增乘客  2.修改乘客
     * @param req
     */
    public void save(ConfirmOrderDoReq req){
        DateTime now = DateTime.now();
        ConfirmOrder confirmOrder = BeanUtil.copyProperties(req, ConfirmOrder.class);
        if (ObjectUtil.isNull(confirmOrder.getId())) {
            confirmOrder.setId(SnowUtil.getSnowflakeNextId());
            confirmOrder.setCreateTime(now);
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.insert(confirmOrder);
        } else {
            confirmOrder.setUpdateTime(now);
//            confirmOrder.setCreateTime(req.getCreateTime());
            confirmOrderMapper.updateByPrimaryKey(confirmOrder);
        }
    }

    /**
     * 乘客查询 1.控制端查询所有乘客  2.business查询当前乘客
     * @param req
     */
    public PageResp<ConfirmOrderQueryResp> queryList(ConfirmOrderQueryReq req){
        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();

        confirmOrderExample.setOrderByClause("id desc");
        ConfirmOrderExample.Criteria criteria = confirmOrderExample.createCriteria();


        // 分页查询语句尽量与需要分页查询的sql语句放在一起，因为其只对最近的一条select语句生效
        LOG.info("页数：{}", req.getPage());
        LOG.info("每页大小：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<ConfirmOrder> confirmOrderList = confirmOrderMapper.selectByExample(confirmOrderExample);

        // 获取总数
        PageInfo<ConfirmOrder> confirmOrderPageInfo = new PageInfo<>(confirmOrderList);
        LOG.info("总行数：{}", confirmOrderPageInfo.getTotal());
        LOG.info("总页数：{}", confirmOrderPageInfo.getPages());

        List<ConfirmOrderQueryResp> confirmOrderQueryRespList = BeanUtil.copyToList(confirmOrderList, ConfirmOrderQueryResp.class);

        PageResp<ConfirmOrderQueryResp> objectPageResp = new PageResp<>();
        objectPageResp.setTotal(confirmOrderPageInfo.getTotal());
        objectPageResp.setList(confirmOrderQueryRespList);
        return objectPageResp;
    }

    /**
     * 乘客删除
     * @param id
     */
    public void delete(Long id){
        confirmOrderMapper.deleteByPrimaryKey(id);
    }

    public void doConfirm(ConfirmOrderDoReq req){
        DateTime now = DateTime.now();
        // 省略数据校验（req中数据合法性校验）， 业务校验，比如今天不能买昨天的票，同乘客同车次不同重复买票

        // 保存确认订单，状态初始
        ConfirmOrder confirmOrder = new ConfirmOrder();
        confirmOrder.setId(SnowUtil.getSnowflakeNextId());
        confirmOrder.setMemberId(LoginMemberContext.getMemberId());
        confirmOrder.setDate(req.getDate());
        confirmOrder.setTrainCode(req.getTrainCode());
        confirmOrder.setStart(req.getStart());
        confirmOrder.setEnd(req.getEnd());
        confirmOrder.setDailyTrainTicketId(req.getDailyTrainTicketId());
        confirmOrder.setStatus(ConfirmOrderStatusEnum.INIT.getCode());
        confirmOrder.setTickets(JSON.toJSONString(req.getTickets()));
        confirmOrder.setCreateTime(now);
        confirmOrder.setUpdateTime(now);
        confirmOrderMapper.insert(confirmOrder);
        // 查询余票记录，得到真实的库存

        // 扣减余票库存，判断余票是否足够

        // 选座
            // 遍历车厢获取座位数据
            // 调休符合条件的座位（多个选座应该在同一个车厢

        // 选中座位后事务处理
            // 座位表售卖情况修改
            // 余票详情表修改余票
            // 为会员增加购票记录
            // 更新确认订单表
    }
}
