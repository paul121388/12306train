package com.jiawa.train.business.service;

import com.jiawa.train.business.domain.DailyTrainSeat;
import com.jiawa.train.business.domain.DailyTrainTicket;
import com.jiawa.train.business.mapper.DailyTrainSeatMapper;
import com.jiawa.train.business.mapper.customer.DailyTrainTicketMapperCustomer;
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
    @Resource
    private DailyTrainTicketMapperCustomer dailyTrainTicketMapperCustomer;

    /**
     * 确认订单后处理
     *
     * @param finalSeatList
     */
//    事务生效：调用别的类的方法，调用本类中的方法不生效

/*
选中座位后事务处理
    座位表售卖情况修改
    余票详情表修改余票
    为会员增加购票记录
    更新确认订单表
*/
    @Transactional
    public void afterDoConfirm(DailyTrainTicket dailyTrainTicket, List<DailyTrainSeat> finalSeatList) {

        // 座位表售卖情况修改，更新部分数据库中部分字段
        for (DailyTrainSeat dailyTrainSeat : finalSeatList) {
            DailyTrainSeat sellSeat = new DailyTrainSeat();
            // 根据主键更新，需要给主键id赋值
            sellSeat.setId(dailyTrainSeat.getId());
            sellSeat.setSell(dailyTrainSeat.getSell());
            sellSeat.setUpdateTime(new Date());
            // 座位表售卖情况修改，更新部分数据库中部分字段
            dailyTrainSeatMapper.updateByPrimaryKeySelective(sellSeat);

            // 余票详情表修改余票
            // 影响的库存：未售卖的余票中和本次购买区间有交集的余票
            Integer startIndex = dailyTrainTicket.getStartIndex();
            Integer endIndex = dailyTrainTicket.getEndIndex();
            Integer minEndIndex = startIndex + 1;
            Integer maxStartIndex = endIndex - 1;

            char[] chars = sellSeat.getSell().toCharArray();
            Integer minStartIndex = 0;
            for (int i = startIndex - 1; i >= 0; i--) {
                char c = chars[i];
                if (c == '1') {
                    minStartIndex = i + 1;
                    break;
                }
            }
            LOG.info("影响的出发站区间：" + minStartIndex + "~" + maxStartIndex);

            Integer maxEndIndex = chars.length;
            for (int i = endIndex; i < chars.length; i++) {
                char c = chars[i];
                if (c == '1') {
                    maxEndIndex = i;
                    break;
                }
            }
            LOG.info("影响的到达站区间：" + minEndIndex + "~" + maxEndIndex);

            dailyTrainTicketMapperCustomer.updateCountBySell(
                    dailyTrainSeat.getDate(),
                    dailyTrainSeat.getTrainCode(),
                    dailyTrainSeat.getSeatType(),
                    minStartIndex,
                    maxStartIndex,
                    minEndIndex,
                    maxEndIndex);
        }


    }


}
