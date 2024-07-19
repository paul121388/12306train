package com.jiawa.train.batch.job;

import cn.hutool.core.util.RandomUtil;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * 禁止并发执行
 */
@DisallowConcurrentExecution
public class DailyTrainJob implements Job {
    private static Logger LOG = LoggerFactory.getLogger(DailyTrainJob.class);
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //增加日志流水号
        MDC.put("LOG_ID", System.currentTimeMillis() + RandomUtil.randomString(3));
        LOG.info("执行每日任务开始");
        LOG.info("执行每日任务结束");
    }
}
