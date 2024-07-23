package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.*;
import com.jiawa.train.business.enums.SeatTypeEnum;
import com.jiawa.train.business.mapper.DailyTrainSeatMapper;
import com.jiawa.train.business.req.DailyTrainSeatQueryReq;
import com.jiawa.train.business.req.DailyTrainSeatSaveReq;
import com.jiawa.train.business.resp.DailyTrainSeatQueryResp;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DailyTrainSeatService {
    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainSeatService.class);

    @Resource
    private DailyTrainSeatMapper dailyTrainSeatMapper;
    @Resource
    private TrainSeatService trainSeatService;
    @Resource
    private TrainStationService trainStationService;

    /**
     * 1.新增乘客  2.修改乘客
     * @param req
     */
    public void save(DailyTrainSeatSaveReq req){
        DateTime now = DateTime.now();
        DailyTrainSeat dailyTrainSeat = BeanUtil.copyProperties(req, DailyTrainSeat.class);
        if (ObjectUtil.isNull(dailyTrainSeat.getId())) {
            dailyTrainSeat.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainSeat.setCreateTime(now);
            dailyTrainSeat.setUpdateTime(now);
            dailyTrainSeatMapper.insert(dailyTrainSeat);
        } else {
            dailyTrainSeat.setUpdateTime(now);
            dailyTrainSeat.setCreateTime(req.getCreateTime());
            dailyTrainSeatMapper.updateByPrimaryKey(dailyTrainSeat);
        }
    }

    /**
     * 乘客查询 1.控制端查询所有乘客  2.business查询当前乘客
     * @param req
     */
    public PageResp<DailyTrainSeatQueryResp> queryList(DailyTrainSeatQueryReq req){
        DailyTrainSeatExample dailyTrainSeatExample = new DailyTrainSeatExample();

        dailyTrainSeatExample.setOrderByClause("train_code asc, carriage_index asc, carriage_seat_index asc");
        DailyTrainSeatExample.Criteria criteria = dailyTrainSeatExample.createCriteria();

        if (ObjectUtil.isNotNull(req.getDate())) {
            criteria.andDateEqualTo(req.getDate());
        }

        if (StrUtil.isNotEmpty(req.getTrainCode())) {
            criteria.andTrainCodeEqualTo(req.getTrainCode());
        }

        // 分页查询语句尽量与需要分页查询的sql语句放在一起，因为其只对最近的一条select语句生效
        LOG.info("页数：{}", req.getPage());
        LOG.info("每页大小：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrainSeat> dailyTrainSeatList = dailyTrainSeatMapper.selectByExample(dailyTrainSeatExample);

        // 获取总数
        PageInfo<DailyTrainSeat> dailyTrainSeatPageInfo = new PageInfo<>(dailyTrainSeatList);
        LOG.info("总行数：{}", dailyTrainSeatPageInfo.getTotal());
        LOG.info("总页数：{}", dailyTrainSeatPageInfo.getPages());

        List<DailyTrainSeatQueryResp> dailyTrainSeatQueryRespList = BeanUtil.copyToList(dailyTrainSeatList, DailyTrainSeatQueryResp.class);

        PageResp<DailyTrainSeatQueryResp> objectPageResp = new PageResp<>();
        objectPageResp.setTotal(dailyTrainSeatPageInfo.getTotal());
        objectPageResp.setList(dailyTrainSeatQueryRespList);
        return objectPageResp;
    }

    /**
     * 乘客删除
     * @param id
     */
    public void delete(Long id){
        dailyTrainSeatMapper.deleteByPrimaryKey(id);
    }

    /**
     * 生成每日火车座位数据
     * @param date
     * @param trainCode
     */
    public void genDaily(Date date, String trainCode) {
        // 打印日志
        LOG.info("开始生成日期【{}】车次为【{}】的座位数据", DateUtil.formatDate(date) , trainCode);
        // 先删除后生成
        // 查出traincode对应的基础车厢信息
        List<TrainSeat> trainSeatList = trainSeatService.selectByTrainCode(trainCode);

        List<TrainStation> stationList = trainStationService.selectByTrainCode(trainCode);
        String sell = StrUtil.fillBefore("", '0',stationList.size()-1);

        if (CollUtil.isEmpty(trainSeatList)) {
            LOG.info("基础数据为空，生成车次为【{}】的座位每日信息失败", trainCode);
            return;
        }

        // 考虑重复生成
        // 首先将数据库中对应车厢数据清空，日期和车厢
        DailyTrainSeatExample dailyTrainSeatExample = new DailyTrainSeatExample();
        dailyTrainSeatExample.createCriteria()
                .andTrainCodeEqualTo(trainCode)
                .andDateEqualTo(date);
        // 执行删除操作
        dailyTrainSeatMapper.deleteByExample(dailyTrainSeatExample);


        // 由于一趟火车有多个，所以需要遍历
        for (TrainSeat trainSeat : trainSeatList) {
            // 生成每天的站点信息
            // 生成date的编号为code的车厢
            DateTime now = DateTime.now();
            // 将train复制到dailyTrainSeat
            DailyTrainSeat dailyTrainSeat = BeanUtil.copyProperties(trainSeat, DailyTrainSeat.class);
            // 设置dailyTrainSeat的id,date,createTime,updateTime
            dailyTrainSeat.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainSeat.setDate(date);
            dailyTrainSeat.setCreateTime(now);
            dailyTrainSeat.setUpdateTime(now);
            // 根据车站信息生成售卖情况
            dailyTrainSeat.setSell(sell);
            // 插入数据库
            dailyTrainSeatMapper.insert(dailyTrainSeat);
            // 打印日志
            LOG.info("生成日期【{}】车次为【{}】的座位结束", DateUtil.formatDate(date) , trainCode);
        }
    }

    public int countSeat(Date date, String trainCode, SeatTypeEnum seatType) {
        DailyTrainSeatExample dailyTrainSeatExample = new DailyTrainSeatExample();
        dailyTrainSeatExample
                .createCriteria()
                .andDateEqualTo(date)
                .andTrainCodeEqualTo(trainCode)
                .andSeatTypeEqualTo(seatType.getCode());
        // 假如结果为0，表明没有这种类型的座位，应该特殊处理，返回-1
        long l = dailyTrainSeatMapper.countByExample(dailyTrainSeatExample);
        if (l == 0) {
            return -1;
        }
        return (int) l;
    }

    public List<DailyTrainSeat> selectByCarriage(Date date, String trainCode, Integer carriageIndex) {
        DailyTrainSeatExample dailyTrainSeatExample = new DailyTrainSeatExample();
        dailyTrainSeatExample.createCriteria()
                .andDateEqualTo(date)
                .andTrainCodeEqualTo(trainCode)
                .andCarriageIndexEqualTo(carriageIndex);
        return dailyTrainSeatMapper.selectByExample(dailyTrainSeatExample);
    }

}
