package com.jiawa.train.exception;

public enum BusinessExceptionEnum {
    MEMBER_MOBILE_EXIST("手机号已经注册过"),
    MEMBERE_MOBILE_NOT_EXIST("手机号不存在，请先获取验证码"),
    MEMBER_CODE_ERROR("验证码错误"),

    BUSSINESS_STATION_NAME_UNIQUE_ERROR("站点名称重复"),
    BUSSINESS_TRAINCARRIAGE_TRAINCODE_INDEX_UNIQUE_ERROR("车厢号重复"),
    BUSSINESS_TRAINSTATION_TRAINCODE_INDEX_UNIQUE_ERROR("车站站序重复"),
    BUSSINESS_TRAINSTATION_TRAINCODE_NAME_UNIQUE_ERROR("车站站名重复"),
    BUSSINESS_TRAIN_CODE_UNIQUE_ERROR("车次编号重复"),


    BUSSINESS_ORDER_TICKET_COUNT_ERROR("余票不足"),
    CONFIRM_ORDER_EXCEPTION("服务器忙，请稍后重试"),
    CONFIRM_ORDER_LOCK_FAIL("当前抢票人数过多，请稍后重试");

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
