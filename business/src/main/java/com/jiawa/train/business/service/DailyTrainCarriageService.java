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
import com.jiawa.train.business.enums.SeatColEnum;
import com.jiawa.train.business.mapper.DailyTrainCarriageMapper;
import com.jiawa.train.business.req.DailyTrainCarriageQueryReq;
import com.jiawa.train.business.req.DailyTrainCarriageSaveReq;
import com.jiawa.train.business.resp.DailyTrainCarriageQueryResp;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DailyTrainCarriageService {
    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainCarriageService.class);

    @Resource
    private DailyTrainCarriageMapper dailyTrainCarriageMapper;

    @Resource
    private TrainCarriageService trainCarriageService;

    /**
     * 1.新增乘客  2.修改乘客
     *
     * @param req
     */
    public void save(DailyTrainCarriageSaveReq req) {
        DateTime now = DateTime.now();

        // 根据座位类型获取列数，自动计算座位数总和
        String seatType = req.getSeatType();
        List<SeatColEnum> colsByType = SeatColEnum.getColsByType(seatType);
        req.setSeatCount(colsByType.size() * req.getRowCount());
        req.setColCount(colsByType.size());

        DailyTrainCarriage dailyTrainCarriage = BeanUtil.copyProperties(req, DailyTrainCarriage.class);

        if (ObjectUtil.isNull(dailyTrainCarriage.getId())) {
            dailyTrainCarriage.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainCarriage.setCreateTime(now);
            dailyTrainCarriage.setUpdateTime(now);
            dailyTrainCarriageMapper.insert(dailyTrainCarriage);
        } else {
            dailyTrainCarriage.setUpdateTime(now);
            dailyTrainCarriage.setCreateTime(req.getCreateTime());
            dailyTrainCarriageMapper.updateByPrimaryKey(dailyTrainCarriage);
        }
    }

    /**
     * 乘客查询 1.控制端查询所有乘客  2.business查询当前乘客
     *
     * @param req
     */
    public PageResp<DailyTrainCarriageQueryResp> queryList(DailyTrainCarriageQueryReq req) {
        DailyTrainCarriageExample dailyTrainCarriageExample = new DailyTrainCarriageExample();

        dailyTrainCarriageExample.setOrderByClause("id desc");
        DailyTrainCarriageExample.Criteria criteria = dailyTrainCarriageExample.createCriteria();

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
        List<DailyTrainCarriage> dailyTrainCarriageList = dailyTrainCarriageMapper.selectByExample(dailyTrainCarriageExample);

        // 获取总数
        PageInfo<DailyTrainCarriage> dailyTrainCarriagePageInfo = new PageInfo<>(dailyTrainCarriageList);
        LOG.info("总行数：{}", dailyTrainCarriagePageInfo.getTotal());
        LOG.info("总页数：{}", dailyTrainCarriagePageInfo.getPages());

        List<DailyTrainCarriageQueryResp> dailyTrainCarriageQueryRespList = BeanUtil.copyToList(dailyTrainCarriageList, DailyTrainCarriageQueryResp.class);

        PageResp<DailyTrainCarriageQueryResp> objectPageResp = new PageResp<>();
        objectPageResp.setTotal(dailyTrainCarriagePageInfo.getTotal());
        objectPageResp.setList(dailyTrainCarriageQueryRespList);
        return objectPageResp;
    }

    /**
     * 乘客删除
     *
     * @param id
     */
    public void delete(Long id) {
        dailyTrainCarriageMapper.deleteByPrimaryKey(id);
    }

    /**
     * 生成每日火车车厢数据
     * @param date
     * @param trainCode
     */
    public void genDaily(Date date, String trainCode) {
        // 打印日志
        LOG.info("开始生成日期【{}】车次为【{}】的车厢数据", DateUtil.formatDate(date) , trainCode);
        // 先删除后生成
        // 查出traincode对应的基础车厢信息
        List<TrainCarriage> trainCarriageList = trainCarriageService.selectByTrainCode(trainCode);

        if (CollUtil.isEmpty(trainCarriageList)) {
            LOG.info("基础数据为空，生成车厢【{}】车站每日信息失败", trainCode);
            return;
        }

        // 考虑重复生成
        // 首先将数据库中对应车厢数据清空，日期和车厢
        DailyTrainCarriageExample dailyTrainCarriageExample = new DailyTrainCarriageExample();
        dailyTrainCarriageExample.createCriteria()
                .andTrainCodeEqualTo(trainCode)
                .andDateEqualTo(date);
        // 执行删除操作
        dailyTrainCarriageMapper.deleteByExample(dailyTrainCarriageExample);


        // 由于一趟车厢有多个站点，所以需要遍历
        for (TrainCarriage trainCarriage : trainCarriageList) {
            // 生成每天的站点信息
            // 生成date的编号为code的车厢
            DateTime now = DateTime.now();
            // 将train复制到dailyTrainCarriage
            DailyTrainCarriage dailyTrainCarriage = BeanUtil.copyProperties(trainCarriage, DailyTrainCarriage.class);
            // 设置dailyTrainCarriage的id,date,createTime,updateTime
            dailyTrainCarriage.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainCarriage.setDate(date);
            dailyTrainCarriage.setCreateTime(now);
            dailyTrainCarriage.setUpdateTime(now);
            // 插入数据库
            dailyTrainCarriageMapper.insert(dailyTrainCarriage);
            // 打印日志
            LOG.info("生成日期【{}】车次为【{}】的车厢数据结束", DateUtil.formatDate(date) , trainCode);
        }
    }
}
