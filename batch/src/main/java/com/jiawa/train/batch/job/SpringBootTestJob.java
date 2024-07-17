package com.jiawa.train.batch.job;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 不适合集群
 * 不能实时更改定时任务状态和策略
 */
@Component
@EnableScheduling
public class SpringBootTestJob {

    @Scheduled(cron = "0/5 * * * * ?")
    public void test() {
        System.out.println("SpringBootTestJob Test");
    }
}
