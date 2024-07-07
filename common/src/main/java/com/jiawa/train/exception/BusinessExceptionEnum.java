package com.jiawa.train.exception;

public enum BusinessExceptionEnum {
    MEMBER_MOBILE_EXIST("手机号已经注册过"),
    MEMBERE_MOBILE_NOT_EXIST("手机号不存在，请先获取验证码"),
    MEMBER_CODE_ERROR("验证码错误");

    private String desc;

    BusinessExceptionEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "BusinessExceptionEnum{" +
                "desc='" + desc + '\'' +
                '}';
    }
}
