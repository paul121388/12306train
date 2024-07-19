package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.DailyTrainSeat;
import com.jiawa.train.business.domain.DailyTrainSeatExample;
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

import java.util.List;

@Service
public class DailyTrainSeatService {
    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainSeatService.class);

    @Resource
    private DailyTrainSeatMapper dailyTrainSeatMapper;

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

        if(StrUtil.isNotBlank(req.getTrainCode())) {
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
}
