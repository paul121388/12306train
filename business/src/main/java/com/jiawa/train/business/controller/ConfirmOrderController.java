package com.jiawa.train.business.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.business.service.BeforeConfirmOrderService;
import com.jiawa.train.business.service.ConfirmOrderService;
import com.jiawa.train.exception.BusinessExceptionEnum;
import com.jiawa.train.resp.CommonResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/confirm-order")
public class ConfirmOrderController<businessLoginReq> {
    @Resource
    private BeforeConfirmOrderService beforeConfirmOrderService;
    @Resource
    private ConfirmOrderService confirmOrderService;
    @Value("${spring.profiles.active}")
    private String env;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);


    /**
     * 保存
     *
     * @param businessRegisterReq
     * @return
     */
    @PostMapping("/do")
    // 接口资源名称不要和接口路径一致，会导致限流后走不到降级方法中
    @SentinelResource(value = "confirmOrderDo", blockHandler = "doConfirmBlockHandler")
    public CommonResp<String> doConfirm(@Valid @RequestBody ConfirmOrderDoReq businessRegisterReq) {
        if (!env.equals("dev")) {
            // 图形验证码校验
            String imageCodeToken = businessRegisterReq.getImageCodeToken();
            String imageCode = businessRegisterReq.getImageCode();
            String imageCodeRedis = redisTemplate.opsForValue().get(imageCodeToken);
            LOG.info("从redis中获取到的验证码：{}", imageCodeRedis);
            if (ObjectUtils.isEmpty(imageCodeRedis)) {
                return new CommonResp<>(false, "验证码已过期", null);
            }
            // 验证码校验，大小写忽略，提升体验，比如Oo Vv Ww容易混
            if (!imageCodeRedis.equalsIgnoreCase(imageCode)) {
                return new CommonResp<>(false, "验证码不正确", null);
            } else {
                // 验证通过后，移除验证码
                redisTemplate.delete(imageCodeToken);
            }
        }
        Long id = beforeConfirmOrderService.doConfirm(businessRegisterReq);
        return new CommonResp<>(String.valueOf(id));
    }

    /**
     * 增加排队查询接口，根据ID查询排队信息
     */
    @GetMapping("/query-line-count/{id}")
    public CommonResp<Integer> queryLineCount(@PathVariable Long id) {
        Integer count = confirmOrderService.queryLineCount(id);
        return new CommonResp<>(count);
    }

    /**
     * 降级方法
     *
     * @param req
     * @param e
     */
    public CommonResp<Object> doConfirmBlockHandler(ConfirmOrderDoReq req, BlockException e) {
        LOG.info("购票请求被限流");
//        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
        CommonResp<Object> commonResp = new CommonResp<>();
        commonResp.setSuccess(false);
        commonResp.setMessage(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION.getDesc());
        return commonResp;
    }


}
