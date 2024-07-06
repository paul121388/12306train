package com.jiawa.train.util;

import cn.hutool.core.util.IdUtil;

public class SnowUtil {
    private static Long datacenterId = 1L;
    private static Long workerId = 1L;

    public static Long getSnowflakeNextId(){
        return IdUtil.getSnowflake(workerId, datacenterId).nextId();
    }

    public static String getSnowflakeNextIdStr(){
        return IdUtil.getSnowflake(workerId, datacenterId).nextIdStr();
    }
}
