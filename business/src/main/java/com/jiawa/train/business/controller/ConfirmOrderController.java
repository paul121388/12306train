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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/confirm-order")
public class ConfirmOrderController<businessLoginReq> {
    @Resource
    private BeforeConfirmOrderService beforeConfirmOrderService;

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
    public CommonResp<Object> doConfirm(@Valid @RequestBody ConfirmOrderDoReq businessRegisterReq) {
        beforeConfirmOrderService.doConfirm(businessRegisterReq);
        return new CommonResp<>();
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
