package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.*;
import com.jiawa.train.business.enums.SeatTypeEnum;
import com.jiawa.train.business.enums.TrainTypeEnum;
import com.jiawa.train.business.mapper.DailyTrainTicketMapper;
import com.jiawa.train.business.req.DailyTrainTicketQueryReq;
import com.jiawa.train.business.req.DailyTrainTicketSaveReq;
import com.jiawa.train.business.resp.DailyTrainTicketQueryResp;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Service
public class DailyTrainTicketService {
    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainTicketService.class);

    @Resource
    private DailyTrainTicketMapper dailyTrainTicketMapper;
    @Resource
    private TrainStationService trainStationService;
    @Resource
    private DailyTrainSeatService dailyTrainSeatService;

    /**
     * 1.新增乘客  2.修改乘客
     * @param req
     */
    public void save(DailyTrainTicketSaveReq req){
        DateTime now = DateTime.now();
        DailyTrainTicket dailyTrainTicket = BeanUtil.copyProperties(req, DailyTrainTicket.class);
        if (ObjectUtil.isNull(dailyTrainTicket.getId())) {
            dailyTrainTicket.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainTicket.setCreateTime(now);
            dailyTrainTicket.setUpdateTime(now);
            dailyTrainTicketMapper.insert(dailyTrainTicket);
        } else {
            dailyTrainTicket.setUpdateTime(now);
            dailyTrainTicket.setCreateTime(req.getCreateTime());
            dailyTrainTicketMapper.updateByPrimaryKey(dailyTrainTicket);
        }
    }

    /**
     * 乘客查询 1.控制端查询所有余票（增加条件查询）
     * @param req
     */
    @Cacheable(value = "DailyTrainTicketService.queryList")
    public PageResp<DailyTrainTicketQueryResp> queryList(DailyTrainTicketQueryReq req){
        DailyTrainTicketExample dailyTrainTicketExample = new DailyTrainTicketExample();

        dailyTrainTicketExample.setOrderByClause("id desc");
        DailyTrainTicketExample.Criteria criteria = dailyTrainTicketExample.createCriteria();

        // 增加查询条件
        if(ObjectUtil.isNotNull(req.getDate())){
            criteria.andDateEqualTo(req.getDate());
        }
        if(StrUtil.isNotEmpty(req.getTrainCode())){
            criteria.andTrainCodeEqualTo(req.getTrainCode());
        }
        if(StrUtil.isNotEmpty(req.getStart())){
            criteria.andStartEqualTo(req.getStart());
        }
        if(StrUtil.isNotEmpty(req.getEnd())){
            criteria.andEndEqualTo(req.getEnd());
        }

        // 分页查询语句尽量与需要分页查询的sql语句放在一起，因为其只对最近的一条select语句生效
        LOG.info("页数：{}", req.getPage());
        LOG.info("每页大小：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrainTicket> dailyTrainTicketList = dailyTrainTicketMapper.selectByExample(dailyTrainTicketExample);

        // 获取总数
        PageInfo<DailyTrainTicket> dailyTrainTicketPageInfo = new PageInfo<>(dailyTrainTicketList);
        LOG.info("总行数：{}", dailyTrainTicketPageInfo.getTotal());
        LOG.info("总页数：{}", dailyTrainTicketPageInfo.getPages());

        List<DailyTrainTicketQueryResp> dailyTrainTicketQueryRespList = BeanUtil.copyToList(dailyTrainTicketList, DailyTrainTicketQueryResp.class);

        PageResp<DailyTrainTicketQueryResp> objectPageResp = new PageResp<>();
        objectPageResp.setTotal(dailyTrainTicketPageInfo.getTotal());
        objectPageResp.setList(dailyTrainTicketQueryRespList);
        return objectPageResp;
    }

    /**
     * 乘客删除
     * @param id
     */
    public void delete(Long id){
        dailyTrainTicketMapper.deleteByPrimaryKey(id);
    }


    /**
     * 生成每日余票信息
     * @param dailyTrain 用于获取trainType，计算票价
     * @param date
     * @param trainCode
     */
    @Transactional
    public void genDaily(DailyTrain dailyTrain, Date date, String trainCode) {
        LOG.info("开始生成日期【{}】车次为【{}】的余票数据", DateUtil.formatDate(date) , trainCode);
        // 删除（date，traincode）
        // 考虑重复生成
        // 首先将数据库中对应车厢数据清空，日期和车厢
        DailyTrainTicketExample dailyTrainTicketExample = new DailyTrainTicketExample();
        dailyTrainTicketExample.createCriteria()
                .andTrainCodeEqualTo(trainCode)
                .andDateEqualTo(date);
        // 执行删除操作
        dailyTrainTicketMapper.deleteByExample(dailyTrainTicketExample);

        // 查找途径的车站信息（date，traincode）
        // 查出traincode对应的基础车次车站信息
        List<TrainStation> trainStationList = trainStationService.selectByTrainCode(trainCode);

        if (CollUtil.isEmpty(trainStationList)) {
            LOG.info("基础火车车站数据为空，生成车次【{}】每日余票失败", trainCode);
            return;
        }

        DateTime now = DateTime.now();
        // 循环，将每段的出发站和结束站放入数据库
        // 外层：遍历出发站
        for (int i = 0; i < trainStationList.size(); i++) {
            // 内层：从出发站开始遍历得到结束站
            // 得到出发站
            TrainStation start = trainStationList.get(i);

            // 计算累积的里程（BigDecimal）
            BigDecimal sumKM = BigDecimal.ZERO;
            for (int j = i+1; j < trainStationList.size(); j++) {
                // 得到结束站
                TrainStation end = trainStationList.get(j);

                // 累积里程
                sumKM = sumKM.add(end.getKm());

                // 金额的操作BigDecimal 里程*座位类型的单价*列车类型的系数=票价
                // 获取trainType
                String trainType = dailyTrain.getType();
                // 根据trainType获取TrainTypeEnum的系数，使用hutool的工具
                BigDecimal priceRate = EnumUtil.getFieldBy(TrainTypeEnum::getPriceRate, TrainTypeEnum::getCode, trainType);
                // 计算票价
                BigDecimal ydzPrice = sumKM.multiply(SeatTypeEnum.YDZ.getPrice()).multiply(priceRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal edzPrice = sumKM.multiply(SeatTypeEnum.EDZ.getPrice()).multiply(priceRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal rwPrice = sumKM.multiply(SeatTypeEnum.RW.getPrice()).multiply(priceRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal ywPrice = sumKM.multiply(SeatTypeEnum.YW.getPrice()).multiply(priceRate).setScale(2, RoundingMode.HALF_UP);

                // 构造余票信息，设置开始和结束站（及其剩余的数据）
                DailyTrainTicket dailyTrainTicket = new DailyTrainTicket();

                dailyTrainTicket.setId(SnowUtil.getSnowflakeNextId());
                dailyTrainTicket.setDate(date);
                dailyTrainTicket.setTrainCode(trainCode);
                dailyTrainTicket.setStart(start.getName());
                dailyTrainTicket.setStartPinyin(start.getNamePinyin());
                dailyTrainTicket.setStartTime(start.getOutTime());
                dailyTrainTicket.setStartIndex(start.getIndex());
                dailyTrainTicket.setEnd(end.getName());
                dailyTrainTicket.setEndPinyin(end.getNamePinyin());
                dailyTrainTicket.setEndTime(end.getInTime());
                dailyTrainTicket.setEndIndex(end.getIndex());

                // 根据date trainconde seatType计算座位的数量
                // 调用dailyTrainSeatMapper的countByExample查询座位总数
                int ydz = dailyTrainSeatService.countSeat(date, trainCode, SeatTypeEnum.YDZ);
                dailyTrainTicket.setYdz(ydz);
                // 金额的操作BigDecimal
                dailyTrainTicket.setYdzPrice(ydzPrice);

                int edz = dailyTrainSeatService.countSeat(date, trainCode, SeatTypeEnum.EDZ);
                dailyTrainTicket.setEdz(edz);

                dailyTrainTicket.setEdzPrice(edzPrice);

                int rw = dailyTrainSeatService.countSeat(date, trainCode, SeatTypeEnum.RW);
                dailyTrainTicket.setRw(rw);
                dailyTrainTicket.setRwPrice(rwPrice);

                int yw = dailyTrainSeatService.countSeat(date, trainCode, SeatTypeEnum.YW);
                dailyTrainTicket.setYw(yw);
                dailyTrainTicket.setYwPrice(ywPrice);

                dailyTrainTicket.setCreateTime(now);
                dailyTrainTicket.setUpdateTime(now);

                // 保存到数据库
                dailyTrainTicketMapper.insert(dailyTrainTicket);
            }
        }
        // 打印日志
        LOG.info("生成日期【{}】车次为【{}】的余票数据结束", DateUtil.formatDate(date) , trainCode);
    }


    /**
     * 根据唯一键查询
     * @param date
     * @param trainCode
     * @param start
     * @param end
     * @return
     */
    public DailyTrainTicket selectByUnique(Date date, String trainCode, String start, String end) {
        DailyTrainTicketExample dailyTrainTicketExample = new DailyTrainTicketExample();
        dailyTrainTicketExample.createCriteria()
                .andTrainCodeEqualTo(trainCode)
                .andDateEqualTo(date)
                .andStartEqualTo(start)
                .andEndEqualTo(end);
        List<DailyTrainTicket> dailyTrainTicketList = dailyTrainTicketMapper.selectByExample(dailyTrainTicketExample);
        if(CollUtil.isNotEmpty(dailyTrainTicketList)){
            return dailyTrainTicketList.get(0);
        }
        else{
            return null;
        }
    }


}
