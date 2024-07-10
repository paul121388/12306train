package com.jiawa.train.controller;

import com.jiawa.train.resp.CommonResp;
import com.jiawa.train.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class ControllerExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerExceptionHandler.class);


    /**
     * 业务异常处理
     * @param e
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public CommonResp exceptionHandler(BusinessException e) {
        CommonResp commonResp = new CommonResp();
        LOG.error("系统异常", e);
        commonResp.setSuccess(false);
        commonResp.setMessage(e.getE().getDesc());
        return commonResp;
    }

    /**
     * 校验异常处理
     * @param e
     * @return
     */
    @ExceptionHandler(BindException.class)
    @ResponseBody
    public CommonResp exceptionHandler(BindException e) {
        CommonResp commonResp = new CommonResp();
        LOG.error("系统异常", e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
        commonResp.setSuccess(false);
        commonResp.setMessage(e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
        return commonResp;
    }


    /**
     * 通用异常处理
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public CommonResp exceptionHandler(Exception e) {
        CommonResp commonResp = new CommonResp();
        LOG.error("系统异常", e);
        commonResp.setSuccess(false);
        commonResp.setMessage("系统出现异常，请联系管理员");
        return commonResp;
    }
}


