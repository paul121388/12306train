package com.jiawa.train.business.service;

import cn.hutool.core.date.DateTime;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.jiawa.train.business.domain.ConfirmOrder;
import com.jiawa.train.business.dto.ConfirmOrderMQDto;
import com.jiawa.train.business.enums.ConfirmOrderStatusEnum;
import com.jiawa.train.business.enums.RocketMQTopicEnum;
import com.jiawa.train.business.mapper.ConfirmOrderMapper;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.business.req.ConfirmOrderTicketReq;
import com.jiawa.train.context.LoginMemberContext;
import com.jiawa.train.exception.BusinessException;
import com.jiawa.train.exception.BusinessExceptionEnum;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class BeforeConfirmOrderService {
    private static final Logger LOG = LoggerFactory.getLogger(BeforeConfirmOrderService.class);

    @Resource
    private SkTokenService skTokenService;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private ConfirmOrderMapper confirmOrderMapper;
    @Resource
    private ConfirmOrderMQDto confirmOrderMQDto;




    @SentinelResource(value = "doConfirm", blockHandler = "doConfirmBlockHandler")
    public void doConfirm(ConfirmOrderDoReq req) {
        req.setMemberId(LoginMemberContext.getMemberId());
        // 令牌校验
        boolean validSkToken = skTokenService.validSkToken(req.getDate(), req.getTrainCode(), LoginMemberContext.getMemberId());
        if (validSkToken) {
            LOG.info("令牌校验通过");
        } else {
            LOG.info("令牌校验不通过");
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_SK_TOKEN_FAIL);
        }

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
        confirmOrder.setMemberId(req.getMemberId());
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
        // 保存确认订单，状态初始

        // 发送MQ，排队购票
//        req.setLogID(MDC.get("LOG_ID"));
//        String reqJson = JSON.toJSONString(req);
        confirmOrderMQDto.setDate(date);
        confirmOrderMQDto.setTrainCode(trainCode);
        confirmOrderMQDto.setLogId(MDC.get("LOG_ID"));
        String reqJson = JSON.toJSONString(confirmOrderMQDto);
        LOG.info("发送MQ，排队购票 reqJson:{}", reqJson);
        rocketMQTemplate.convertAndSend(RocketMQTopicEnum.CONFIRM_ORDER.getCode(), reqJson);
        LOG.info("排队购票，发送MQ结束");
    }



    public void doConfirmBlockHandler(ConfirmOrderDoReq req, BlockException e) {
        LOG.info("购票请求被限流");
        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
    }
}
