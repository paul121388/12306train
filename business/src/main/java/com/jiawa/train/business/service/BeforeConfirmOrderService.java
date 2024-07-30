package com.jiawa.train.business.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.jiawa.train.business.enums.RedisKeyPreEnum;
import com.jiawa.train.business.enums.RocketMQTopicEnum;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.context.LoginMemberContext;
import com.jiawa.train.exception.BusinessException;
import com.jiawa.train.exception.BusinessExceptionEnum;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class BeforeConfirmOrderService {
    private static final Logger LOG = LoggerFactory.getLogger(BeforeConfirmOrderService.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SkTokenService skTokenService;
    @Resource
    private RocketMQTemplate rocketMQTemplate;




    @SentinelResource(value = "doConfirm", blockHandler = "doConfirmBlockHandler")
    public void doConfirm(ConfirmOrderDoReq req) {

        // 令牌校验
        boolean validSkToken = skTokenService.validSkToken(req.getDate(), req.getTrainCode(), LoginMemberContext.getMemberId());
        if (validSkToken) {
            LOG.info("令牌校验通过");
        } else {
            LOG.info("令牌校验不通过");
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_SK_TOKEN_FAIL);
        }

        String lockKey = RedisKeyPreEnum.CONFIRM_ORDER + "-" + req.getDate() + "-" + req.getTrainCode();
        Boolean setIfAbsent = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockKey, 5, TimeUnit.SECONDS);
        if (setIfAbsent) {
            LOG.info("恭喜抢到锁了! lockKey:{}", lockKey);
        } else {
            LOG.info("抢锁失败! lockKey:{}", lockKey);
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_LOCK_FAIL);
        }

        // 发送MQ，排队购票
        String reqJson = JSON.toJSONString(req);
        LOG.info("发送MQ，排队购票 reqJson:{}", reqJson);
        rocketMQTemplate.convertAndSend(RocketMQTopicEnum.CONFIRM_ORDER.getCode(), reqJson);
        LOG.info("排队购票，发送MQ结束");
    }



    public void doConfirmBlockHandler(ConfirmOrderDoReq req, BlockException e) {
        LOG.info("购票请求被限流");
        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
    }
}
