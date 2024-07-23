package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.ConfirmOrder;
import com.jiawa.train.business.domain.ConfirmOrderExample;
import com.jiawa.train.business.domain.DailyTrainTicket;
import com.jiawa.train.business.enums.ConfirmOrderStatusEnum;
import com.jiawa.train.business.enums.SeatColEnum;
import com.jiawa.train.business.enums.SeatTypeEnum;
import com.jiawa.train.business.mapper.ConfirmOrderMapper;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.business.req.ConfirmOrderQueryReq;
import com.jiawa.train.business.req.ConfirmOrderTicketReq;
import com.jiawa.train.business.resp.ConfirmOrderQueryResp;
import com.jiawa.train.context.LoginMemberContext;
import com.jiawa.train.exception.BusinessException;
import com.jiawa.train.exception.BusinessExceptionEnum;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ConfirmOrderService {
    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;
    @Resource
    private DailyTrainTicketService dailyTrainTicketService;

    /**
     * 1.新增乘客  2.修改乘客
     *
     * @param req
     */
    public void save(ConfirmOrderDoReq req) {
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
     *
     * @param req
     */
    public PageResp<ConfirmOrderQueryResp> queryList(ConfirmOrderQueryReq req) {
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
     *
     * @param id
     */
    public void delete(Long id) {
        confirmOrderMapper.deleteByPrimaryKey(id);
    }

    public void doConfirm(ConfirmOrderDoReq req) {
        DateTime now = DateTime.now();
        // 省略数据校验（req中数据合法性校验）， 业务校验，比如今天不能买昨天的票，同乘客同车次不同重复买票
        Date date = req.getDate();
        String trainCode = req.getTrainCode();
        String start = req.getStart();
        String end = req.getEnd();
        List<ConfirmOrderTicketReq> confirmOrderTicketReqList = req.getTickets();
        // 保存确认订单，状态初始
        ConfirmOrder confirmOrder = new ConfirmOrder();
        confirmOrder.setId(SnowUtil.getSnowflakeNextId());
        confirmOrder.setMemberId(LoginMemberContext.getMemberId());
        confirmOrder.setDate(date);
        confirmOrder.setTrainCode(trainCode);
        confirmOrder.setStart(start);
        confirmOrder.setEnd(end);
        confirmOrder.setDailyTrainTicketId(req.getDailyTrainTicketId());
        confirmOrder.setStatus(ConfirmOrderStatusEnum.INIT.getCode());
        confirmOrder.setTickets(JSON.toJSONString(confirmOrderTicketReqList));
        confirmOrder.setCreateTime(now);
        confirmOrder.setUpdateTime(now);
        confirmOrderMapper.insert(confirmOrder);

        // 查询余票记录，得到真实的库存
        DailyTrainTicket dailyTrainTicket = dailyTrainTicketService.selectByUnique(date, trainCode, start, end);
        LOG.info("余票记录：{}", dailyTrainTicket);
        // 扣减余票库存，判断余票是否足够
        reduceTicket(confirmOrderTicketReqList, dailyTrainTicket);

        // 计算相对于选定的第一个座位的偏移值（可以选座时）
        // 根据第一张票的座位类型是否为空，判断能否选座
        ConfirmOrderTicketReq ticketReq = confirmOrderTicketReqList.get(0);
        if (StrUtil.isNotBlank(ticketReq.getSeat())) {
            // 可选座时
            LOG.info("可选座");
            // 查出这种座位类型有多少列（不同座位类型，偏移值不同）
            List<SeatColEnum> colEnumList = SeatColEnum.getColsByType(ticketReq.getSeatTypeCode());
            LOG.info("座位类型包含的列：{}", colEnumList);
            // 组成和前端相同的两排座位（循环）
            List<String> referSeatList = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                for (SeatColEnum seatColEnum : colEnumList) {
                    referSeatList.add((seatColEnum.getCode() + (i+1)));
                }
            }
            LOG.info("参考座位：{}", referSeatList);

            // 计算偏移值
            // 1. 绝对偏移值，遍历座位号，获得每一张票的seat
            List<Integer> absOffsetList = new ArrayList<>();
            for (ConfirmOrderTicketReq ticketReq1 : confirmOrderTicketReqList) {
                absOffsetList.add(referSeatList.indexOf(ticketReq1.getSeat()));
            }
            LOG.info("绝对偏移值：{}", absOffsetList);

            // 2. 计算相对偏移值（都减去第一个座位的绝对偏移值）
            List<Integer> relOffsetList = new ArrayList<>();
            for (Integer index: absOffsetList) {
                relOffsetList.add(index - absOffsetList.get(0));
            }
            LOG.info("相对偏移值：{}", relOffsetList);
        } else {
            // 不可选座时
            LOG.info("不可选座");
        }


        // 选座
        // 遍历车厢获取座位数据
        // 调休符合条件的座位（多个选座应该在同一个车厢

        // 选中座位后事务处理
        // 座位表售卖情况修改
        // 余票详情表修改余票
        // 为会员增加购票记录
        // 更新确认订单表
    }

    // 扣减余票库存，判断余票是否足够
    private void reduceTicket(List<ConfirmOrderTicketReq> confirmOrderTicketReqList, DailyTrainTicket dailyTrainTicket) {
        for (ConfirmOrderTicketReq ticketReq : confirmOrderTicketReqList) {
            String seatTypeCode = ticketReq.getSeatTypeCode();
            SeatTypeEnum seatTypeEnum = EnumUtil.getBy(SeatTypeEnum::getCode, seatTypeCode);
            switch (seatTypeEnum) {
                case YDZ -> {
                    int countYDZLeft = dailyTrainTicket.getYdz() - 1;
                    if (countYDZLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.BUSSINESS_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYdz(countYDZLeft);
                }
                case EDZ -> {
                    int countEDZLeft = dailyTrainTicket.getEdz() - 1;
                    if (countEDZLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.BUSSINESS_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setEdz(countEDZLeft);
                }
                case RW -> {
                    int countRWLeft = dailyTrainTicket.getRw() - 1;
                    if (countRWLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.BUSSINESS_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setRw(countRWLeft);
                }
                case YW -> {
                    int countYWLeft = dailyTrainTicket.getYw() - 1;
                    if (countYWLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.BUSSINESS_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYw(countYWLeft);
                }
            }
        }
    }
}
