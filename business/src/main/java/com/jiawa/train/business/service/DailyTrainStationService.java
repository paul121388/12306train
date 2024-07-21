package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.DailyTrainStation;
import com.jiawa.train.business.domain.DailyTrainStationExample;
import com.jiawa.train.business.domain.TrainStation;
import com.jiawa.train.business.mapper.DailyTrainStationMapper;
import com.jiawa.train.business.req.DailyTrainStationQueryReq;
import com.jiawa.train.business.req.DailyTrainStationSaveReq;
import com.jiawa.train.business.resp.DailyTrainStationQueryResp;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DailyTrainStationService {
    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainStationService.class);

    @Resource
    private DailyTrainStationMapper dailyTrainStationMapper;

    @Resource
    private TrainStationService trainStationService;

    /**
     * 1.新增乘客  2.修改乘客
     *
     * @param req
     */
    public void save(DailyTrainStationSaveReq req) {
        DateTime now = DateTime.now();
        DailyTrainStation dailyTrainStation = BeanUtil.copyProperties(req, DailyTrainStation.class);
        if (ObjectUtil.isNull(dailyTrainStation.getId())) {
            dailyTrainStation.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainStation.setCreateTime(now);
            dailyTrainStation.setUpdateTime(now);
            dailyTrainStationMapper.insert(dailyTrainStation);
        } else {
            dailyTrainStation.setUpdateTime(now);
            dailyTrainStation.setCreateTime(req.getCreateTime());
            dailyTrainStationMapper.updateByPrimaryKey(dailyTrainStation);
        }
    }

    /**
     * 乘客查询 1.控制端查询所有乘客  2.business查询当前乘客
     *
     * @param req
     */
    public PageResp<DailyTrainStationQueryResp> queryList(DailyTrainStationQueryReq req) {
        DailyTrainStationExample dailyTrainStationExample = new DailyTrainStationExample();

        dailyTrainStationExample.setOrderByClause("id desc");
        DailyTrainStationExample.Criteria criteria = dailyTrainStationExample.createCriteria();

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
        List<DailyTrainStation> dailyTrainStationList = dailyTrainStationMapper.selectByExample(dailyTrainStationExample);

        // 获取总数
        PageInfo<DailyTrainStation> dailyTrainStationPageInfo = new PageInfo<>(dailyTrainStationList);
        LOG.info("总行数：{}", dailyTrainStationPageInfo.getTotal());
        LOG.info("总页数：{}", dailyTrainStationPageInfo.getPages());

        List<DailyTrainStationQueryResp> dailyTrainStationQueryRespList = BeanUtil.copyToList(dailyTrainStationList, DailyTrainStationQueryResp.class);

        PageResp<DailyTrainStationQueryResp> objectPageResp = new PageResp<>();
        objectPageResp.setTotal(dailyTrainStationPageInfo.getTotal());
        objectPageResp.setList(dailyTrainStationQueryRespList);
        return objectPageResp;
    }

    /**
     * 乘客删除
     *
     * @param id
     */
    public void delete(Long id) {
        dailyTrainStationMapper.deleteByPrimaryKey(id);
    }

    /**
     * 生成当日火车车站数据
     *
     * @param date
     * @param trainCode
     */
    public void genDaily(Date date, String trainCode) {
        // 打印日志
        LOG.info("开始生成日期【{}】车次【{}】的车站数据", DateUtil.formatDate(date) , trainCode);
        // 先删除后生成
        // 查出traincode对应的基础车次车站信息
        List<TrainStation> trainStationList = trainStationService.selectByTrainCode(trainCode);

        if (CollUtil.isEmpty(trainStationList)) {
            LOG.info("基础数据为空，生成车次【{}】车站每日信息失败", trainCode);
            return;
        }

        // 考虑重复生成
        // 首先将数据库中对应车次数据清空，日期和车次
        DailyTrainStationExample dailyTrainStationExample = new DailyTrainStationExample();
        dailyTrainStationExample.createCriteria()
                .andTrainCodeEqualTo(trainCode)
                .andDateEqualTo(date);
        // 执行删除操作
        dailyTrainStationMapper.deleteByExample(dailyTrainStationExample);


        // 由于一趟车次有多个站点，所以需要遍历
        for (TrainStation trainStation : trainStationList) {
            // 生成每天的站点信息
            // 生成date的编号为code的车次
            DateTime now = DateTime.now();
            // 将train复制到dailyTrainStation
            DailyTrainStation dailyTrainStation = BeanUtil.copyProperties(trainStation, DailyTrainStation.class);
            // 设置dailyTrainStation的id,date,createTime,updateTime
            dailyTrainStation.setId(SnowUtil.getSnowflakeNextId());
            dailyTrainStation.setDate(date);
            dailyTrainStation.setCreateTime(now);
            dailyTrainStation.setUpdateTime(now);
            // 插入数据库
            dailyTrainStationMapper.insert(dailyTrainStation);
            // 打印日志
            LOG.info("生成日期【{}】车次【{}】的车站数据结束", DateUtil.formatDate(date) , trainCode);

        }
    }

}
