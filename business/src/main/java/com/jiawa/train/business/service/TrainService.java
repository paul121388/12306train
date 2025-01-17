package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.Train;
import com.jiawa.train.business.domain.TrainExample;
import com.jiawa.train.business.mapper.TrainMapper;
import com.jiawa.train.business.req.TrainQueryReq;
import com.jiawa.train.business.req.TrainSaveReq;
import com.jiawa.train.business.resp.TrainQueryResp;
import com.jiawa.train.exception.BusinessException;
import com.jiawa.train.exception.BusinessExceptionEnum;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainService {
    private static final Logger LOG = LoggerFactory.getLogger(TrainService.class);

    @Resource
    private TrainMapper trainMapper;

    /**
     * 1.新增列车  2.修改列车
     * @param req
     */
    public void save(TrainSaveReq req){
        DateTime now = DateTime.now();
        Train train = BeanUtil.copyProperties(req, Train.class);
        if (ObjectUtil.isNull(train.getId())) {
            // 保存前，校验唯一键
            Train trainDB = selectByUnique(req.getCode());
            if (ObjectUtil.isNotEmpty(trainDB)) {
                throw new BusinessException(BusinessExceptionEnum.BUSSINESS_TRAIN_CODE_UNIQUE_ERROR);
            }
            train.setId(SnowUtil.getSnowflakeNextId());
            train.setCreateTime(now);
            train.setUpdateTime(now);
            trainMapper.insert(train);
        } else {
            train.setUpdateTime(now);
            train.setCreateTime(req.getCreateTime());
            trainMapper.updateByPrimaryKey(train);
        }
    }

    private Train selectByUnique(String code) {
        TrainExample trainExample = new TrainExample();
        trainExample.createCriteria().andCodeEqualTo(code);
        List<Train> trainList = trainMapper.selectByExample(trainExample);
        if(CollUtil.isNotEmpty(trainList)){
            return trainList.get(0);
        }
        else{
            return null;
        }
    }

    /**
     * 乘客查询 1.控制端查询所有列车  2.business查询当前列车
     * @param req
     */
    public PageResp<TrainQueryResp> queryList(TrainQueryReq req){
        TrainExample trainExample = new TrainExample();

        trainExample.setOrderByClause("id desc");
        TrainExample.Criteria criteria = trainExample.createCriteria();


        // 分页查询语句尽量与需要分页查询的sql语句放在一起，因为其只对最近的一条select语句生效
        LOG.info("页数：{}", req.getPage());
        LOG.info("每页大小：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<Train> trainList = trainMapper.selectByExample(trainExample);

        // 获取总数
        PageInfo<Train> trainPageInfo = new PageInfo<>(trainList);
        LOG.info("总行数：{}", trainPageInfo.getTotal());
        LOG.info("总页数：{}", trainPageInfo.getPages());

        List<TrainQueryResp> trainQueryRespList = BeanUtil.copyToList(trainList, TrainQueryResp.class);

        PageResp<TrainQueryResp> objectPageResp = new PageResp<>();
        objectPageResp.setTotal(trainPageInfo.getTotal());
        objectPageResp.setList(trainQueryRespList);
        return objectPageResp;
    }

    /**
     * 列车删除
     * @param id
     */
    public void delete(Long id){
        trainMapper.deleteByPrimaryKey(id);
    }

    /**
     * 查询所有列车
     * @param
     * @return
     */
//    @Transactional
    public List<TrainQueryResp> queryAll(){
        List<Train> trainList = selectAll();
//        演示mybatis的缓存
//        LOG.info("再查一次");
//        trainList = selectAll();
        return BeanUtil.copyToList(trainList, TrainQueryResp.class);
    }

    /**
     * 查询所有列车，直接获得数据库的信息
     * @return
     */
//    事务让mybatis的一级缓存生效
//    @Transactional
    public List<Train> selectAll() {
        TrainExample trainExample = new TrainExample();

        trainExample.setOrderByClause("code asc");
        TrainExample.Criteria criteria = trainExample.createCriteria();
//        trainMapper.selectByExample(trainExample);
        return trainMapper.selectByExample(trainExample);
    }
}
