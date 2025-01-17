package com.jiawa.train.exception;

public class BusinessException extends RuntimeException {

    private BusinessExceptionEnum e;

    public BusinessException(BusinessExceptionEnum e) {
        this.e = e;
    }

    public BusinessExceptionEnum getE() {
        return e;
    }

    public void setE(BusinessExceptionEnum e) {
        this.e = e;
    }

    @Override
    public String toString() {
        return "BusinessException{" +
                "e=" + e +
                '}';
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
