package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.TrainStation;
import com.jiawa.train.business.domain.TrainStationExample;
import com.jiawa.train.business.mapper.TrainStationMapper;
import com.jiawa.train.business.req.TrainStationQueryReq;
import com.jiawa.train.business.req.TrainStationSaveReq;
import com.jiawa.train.business.resp.TrainStationQueryResp;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainStationService {
    private static final Logger LOG = LoggerFactory.getLogger(TrainStationService.class);

    @Resource
    private TrainStationMapper trainStationMapper;

    /**
     * 1.新增乘客  2.修改乘客
     * @param req
     */
    public void save(TrainStationSaveReq req){
        DateTime now = DateTime.now();
        TrainStation trainStation = BeanUtil.copyProperties(req, TrainStation.class);
        if (ObjectUtil.isNull(trainStation.getId())) {
            trainStation.setId(SnowUtil.getSnowflakeNextId());
            trainStation.setCreateTime(now);
            trainStation.setUpdateTime(now);
            trainStationMapper.insert(trainStation);
        } else {
            trainStation.setUpdateTime(now);
            trainStation.setCreateTime(req.getCreateTime());
            trainStationMapper.updateByPrimaryKey(trainStation);
        }
    }

    /**
     * 乘客查询 1.控制端查询所有乘客  2.business查询当前乘客
     * @param req
     */
    public PageResp<TrainStationQueryResp> queryList(TrainStationQueryReq req){
        TrainStationExample trainStationExample = new TrainStationExample();

        trainStationExample.setOrderByClause("id desc");
        TrainStationExample.Criteria criteria = trainStationExample.createCriteria();


        // 分页查询语句尽量与需要分页查询的sql语句放在一起，因为其只对最近的一条select语句生效
        LOG.info("页数：{}", req.getPage());
        LOG.info("每页大小：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<TrainStation> trainStationList = trainStationMapper.selectByExample(trainStationExample);

        // 获取总数
        PageInfo<TrainStation> trainStationPageInfo = new PageInfo<>(trainStationList);
        LOG.info("总行数：{}", trainStationPageInfo.getTotal());
        LOG.info("总页数：{}", trainStationPageInfo.getPages());

        List<TrainStationQueryResp> trainStationQueryRespList = BeanUtil.copyToList(trainStationList, TrainStationQueryResp.class);

        PageResp<TrainStationQueryResp> objectPageResp = new PageResp<>();
        objectPageResp.setTotal(trainStationPageInfo.getTotal());
        objectPageResp.setList(trainStationQueryRespList);
        return objectPageResp;
    }

    /**
     * 乘客删除
     * @param id
     */
    public void delete(Long id){
        trainStationMapper.deleteByPrimaryKey(id);
    }
}
