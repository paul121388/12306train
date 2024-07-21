package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.DailyTrain;
import com.jiawa.train.business.domain.DailyTrainExample;
import com.jiawa.train.business.domain.Train;
import com.jiawa.train.business.mapper.DailyTrainMapper;
import com.jiawa.train.business.req.DailyTrainQueryReq;
import com.jiawa.train.business.req.DailyTrainSaveReq;
import com.jiawa.train.business.resp.DailyTrainQueryResp;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DailyTrainService {
    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainService.class);

    @Resource
    private DailyTrainMapper dailyTrainMapper;
    @Resource
    private TrainService trainService;
    @Resource
    private DailyTrainStationService dailyTrainStationService;
    @Resource
    private DailyTrainCarriageService dailyTrainCarriageService;


    /**
     * 1.新增乘客  2.修改乘客
     *
     * @param req
     */
    public void save(DailyTrainSaveReq req) {
        DateTime now = DateTime.now();
        DailyTrain dailyTrain = BeanUtil.copyProperties(req, DailyTrain.class);
        if (ObjectUtil.isNull(dailyTrain.getId())) {
            LOG.info("新增列车");
            dailyTrain.setId(SnowUtil.getSnowflakeNextId());
            dailyTrain.setCreateTime(now);
            dailyTrain.setUpdateTime(now);
            dailyTrainMapper.insert(dailyTrain);
        } else {
            LOG.info("修改列车");
            dailyTrain.setUpdateTime(now);
            dailyTrain.setCreateTime(req.getCreateTime());
            dailyTrainMapper.updateByPrimaryKey(dailyTrain);
        }
    }

    /**
     * 乘客查询 1.控制端查询所有乘客  2.business查询当前乘客
     *
     * @param req
     */
    public PageResp<DailyTrainQueryResp> queryList(DailyTrainQueryReq req) {
        DailyTrainExample dailyTrainExample = new DailyTrainExample();

        dailyTrainExample.setOrderByClause("id desc");
        DailyTrainExample.Criteria criteria = dailyTrainExample.createCriteria();

        if (ObjectUtil.isNotNull(req.getDate())) {
            criteria.andCreateTimeGreaterThanOrEqualTo(req.getDate());
        }

        if (StrUtil.isNotEmpty(req.getCode())) {
            criteria.andCodeEqualTo(req.getCode());
        }

        // 分页查询语句尽量与需要分页查询的sql语句放在一起，因为其只对最近的一条select语句生效
        LOG.info("页数：{}", req.getPage());
        LOG.info("每页大小：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrain> dailyTrainList = dailyTrainMapper.selectByExample(dailyTrainExample);

        // 获取总数
        PageInfo<DailyTrain> dailyTrainPageInfo = new PageInfo<>(dailyTrainList);
        LOG.info("总行数：{}", dailyTrainPageInfo.getTotal());
        LOG.info("总页数：{}", dailyTrainPageInfo.getPages());

        List<DailyTrainQueryResp> dailyTrainQueryRespList = BeanUtil.copyToList(dailyTrainList, DailyTrainQueryResp.class);

        PageResp<DailyTrainQueryResp> objectPageResp = new PageResp<>();
        objectPageResp.setTotal(dailyTrainPageInfo.getTotal());
        objectPageResp.setList(dailyTrainQueryRespList);
        return objectPageResp;
    }

    /**
     * 乘客删除
     *
     * @param id
     */
    public void delete(Long id) {
        dailyTrainMapper.deleteByPrimaryKey(id);
    }

    /**
     * 生成每日所有车次，车次、车站、车厢、座位
     *
     * @param date
     */
    public void genDaily(Date date) {
        // 查询基础数据
        List<Train> trainList = trainService.selectAll();
        // 使用列表之前判断是否为空
        if (CollUtil.isEmpty(trainList)) {
            LOG.info("火车基础数据为空");
            return;
        }

        // 拿到车次基础数据后，循环生成车次
        for (Train train : trainList) {
            genDailyTrain(date, train);
        }
    }

    private void genDailyTrain(Date date, Train train) {
        // 打印日志
        LOG.info("开始生成日期【{}】车次【{}】数据", DateUtil.formatDate(date) , train.getCode());
        // 考虑重复生成
        // 首先将数据库中对应车次数据清空，日期和车次
        DailyTrainExample dailyTrainExample = new DailyTrainExample();
        dailyTrainExample.createCriteria()
                .andCodeEqualTo(train.getCode())
                .andDateEqualTo(date);
        // 执行删除操作
        dailyTrainMapper.deleteByExample(dailyTrainExample);

        // 生成date的编号为code的车次
        DateTime now = DateTime.now();
        // 将train复制到dailyTrain
        DailyTrain dailyTrain = BeanUtil.copyProperties(train, DailyTrain.class);
        // 设置dailyTrain的id,date,createTime,updateTime
        dailyTrain.setId(SnowUtil.getSnowflakeNextId());
        dailyTrain.setDate(date);
        dailyTrain.setCreateTime(now);
        dailyTrain.setUpdateTime(now);
        // 插入数据库
        dailyTrainMapper.insert(dailyTrain);

        // 生成date的编号为code的车次车站数据
        dailyTrainStationService.genDaily(date, train.getCode());

        // 生成date的编号为code的车次车厢数据
        dailyTrainCarriageService.genDaily(date, train.getCode());
        // 打印日志
        LOG.info("生成日期【{}】车次【{}】数据结束", DateUtil.formatDate(date) , train.getCode());
    }
}
