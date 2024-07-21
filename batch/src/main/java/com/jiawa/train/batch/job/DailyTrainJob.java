package com.jiawa.train.batch.job;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.jiawa.train.batch.feign.BusinessFeign;
import jakarta.annotation.Resource;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Date;

/**
 * 禁止并发执行
 */
@DisallowConcurrentExecution
public class DailyTrainJob implements Job {
    private static Logger LOG = LoggerFactory.getLogger(DailyTrainJob.class);

//    注入businessFeign
    @Resource
    BusinessFeign businessFeign;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //增加日志流水号
        MDC.put("LOG_ID", System.currentTimeMillis() + RandomUtil.randomString(3));
        LOG.info("生成15天后的车次数据，开始");
        // 计算日期
        // 得到当前的日期并偏置15天
        Date date = new Date();
        DateTime dateTime = DateUtil.offsetDay(date, 15);
        // 将hutool的datetime转换为java的date
        Date offsetDate = dateTime.toJdkDate();
        // 通过businessFeign调用DailyTrainAdminController的接口
        businessFeign.genDaily(offsetDate);
        LOG.info("生成15天后的车次数据，结束");
    }
}
