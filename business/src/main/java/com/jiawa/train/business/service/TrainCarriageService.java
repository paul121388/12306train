package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.TrainCarriage;
import com.jiawa.train.business.domain.TrainCarriageExample;
import com.jiawa.train.business.mapper.TrainCarriageMapper;
import com.jiawa.train.business.req.TrainCarriageQueryReq;
import com.jiawa.train.business.req.TrainCarriageSaveReq;
import com.jiawa.train.business.resp.TrainCarriageQueryResp;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainCarriageService {
    private static final Logger LOG = LoggerFactory.getLogger(TrainCarriageService.class);

    @Resource
    private TrainCarriageMapper trainCarriageMapper;

    /**
     * 1.新增乘客  2.修改乘客
     * @param req
     */
    public void save(TrainCarriageSaveReq req){
        DateTime now = DateTime.now();
        TrainCarriage trainCarriage = BeanUtil.copyProperties(req, TrainCarriage.class);
        if (ObjectUtil.isNull(trainCarriage.getId())) {
            trainCarriage.setId(SnowUtil.getSnowflakeNextId());
            trainCarriage.setCreateTime(now);
            trainCarriage.setUpdateTime(now);
            trainCarriageMapper.insert(trainCarriage);
        } else {
            trainCarriage.setUpdateTime(now);
            trainCarriage.setCreateTime(req.getCreateTime());
            trainCarriageMapper.updateByPrimaryKey(trainCarriage);
        }
    }

    /**
     * 乘客查询 1.控制端查询所有乘客  2.business查询当前乘客
     * @param req
     */
    public PageResp<TrainCarriageQueryResp> queryList(TrainCarriageQueryReq req){
        TrainCarriageExample trainCarriageExample = new TrainCarriageExample();

        trainCarriageExample.setOrderByClause("id desc");
        TrainCarriageExample.Criteria criteria = trainCarriageExample.createCriteria();


        // 分页查询语句尽量与需要分页查询的sql语句放在一起，因为其只对最近的一条select语句生效
        LOG.info("页数：{}", req.getPage());
        LOG.info("每页大小：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<TrainCarriage> trainCarriageList = trainCarriageMapper.selectByExample(trainCarriageExample);

        // 获取总数
        PageInfo<TrainCarriage> trainCarriagePageInfo = new PageInfo<>(trainCarriageList);
        LOG.info("总行数：{}", trainCarriagePageInfo.getTotal());
        LOG.info("总页数：{}", trainCarriagePageInfo.getPages());

        List<TrainCarriageQueryResp> trainCarriageQueryRespList = BeanUtil.copyToList(trainCarriageList, TrainCarriageQueryResp.class);

        PageResp<TrainCarriageQueryResp> objectPageResp = new PageResp<>();
        objectPageResp.setTotal(trainCarriagePageInfo.getTotal());
        objectPageResp.setList(trainCarriageQueryRespList);
        return objectPageResp;
    }

    /**
     * 乘客删除
     * @param id
     */
    public void delete(Long id){
        trainCarriageMapper.deleteByPrimaryKey(id);
    }
}
