package com.jiawa.train.business.service;

import com.jiawa.train.business.domain.DailyTrainSeat;
import com.jiawa.train.business.mapper.DailyTrainSeatMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class AfterConfirmOrderService {
    private static final Logger LOG = LoggerFactory.getLogger(AfterConfirmOrderService.class);

    @Resource
    private DailyTrainSeatMapper dailyTrainSeatMapper;

    /**
     * 确认订单后处理
     * @param finalSeatList
     */
//    事务生效：调用别的类的方法，调用本类中的方法不生效

    /**
     * 选中座位后事务处理
     *      座位表售卖情况修改
     */
    @Transactional
    public void afterDoConfirm(List<DailyTrainSeat> finalSeatList) {
//更新部分数据库中部分字段
        for (DailyTrainSeat dailyTrainSeat : finalSeatList) {
            DailyTrainSeat sellSeat = new DailyTrainSeat();
//            根据主键更新，需要给主键id赋值
            sellSeat.setId(dailyTrainSeat.getId());
            sellSeat.setSell(dailyTrainSeat.getSell());
            sellSeat.setUpdateTime(new Date());
            dailyTrainSeatMapper.updateByPrimaryKeySelective(sellSeat);
        }
    }

}
