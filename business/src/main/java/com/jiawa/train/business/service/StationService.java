package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.Station;
import com.jiawa.train.business.domain.StationExample;
import com.jiawa.train.business.mapper.StationMapper;
import com.jiawa.train.business.req.StationQueryReq;
import com.jiawa.train.business.req.StationSaveReq;
import com.jiawa.train.business.resp.StationQueryResp;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StationService {
    private static final Logger LOG = LoggerFactory.getLogger(StationService.class);

    @Resource
    private StationMapper stationMapper;

    /**
     * 1.新增车站  2.修改车站
     * @param req
     */
    public void save(StationSaveReq req){
        DateTime now = DateTime.now();
        Station station = BeanUtil.copyProperties(req, Station.class);
        if (ObjectUtil.isNull(station.getId())) {
            station.setId(SnowUtil.getSnowflakeNextId());
            station.setCreateTime(now);
            station.setUpdateTime(now);
            stationMapper.insert(station);
        } else {
            station.setUpdateTime(now);
            station.setCreateTime(req.getCreateTime());
            stationMapper.updateByPrimaryKey(station);
        }
    }

    /**
     * 车站查询 1.控制端查询所有车站  2.business查询当前车站
     * @param req
     */
    public PageResp<StationQueryResp> queryList(StationQueryReq req){
        StationExample stationExample = new StationExample();

        stationExample.setOrderByClause("id desc");
        StationExample.Criteria criteria = stationExample.createCriteria();


        // 分页查询语句尽量与需要分页查询的sql语句放在一起，因为其只对最近的一条select语句生效
        LOG.info("页数：{}", req.getPage());
        LOG.info("每页大小：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<Station> stationList = stationMapper.selectByExample(stationExample);

        // 获取总数
        PageInfo<Station> stationPageInfo = new PageInfo<>(stationList);
        LOG.info("总行数：{}", stationPageInfo.getTotal());
        LOG.info("总页数：{}", stationPageInfo.getPages());

        List<StationQueryResp> stationQueryRespList = BeanUtil.copyToList(stationList, StationQueryResp.class);

        PageResp<StationQueryResp> objectPageResp = new PageResp<>();
        objectPageResp.setTotal(stationPageInfo.getTotal());
        objectPageResp.setList(stationQueryRespList);
        return objectPageResp;
    }

    /**
     * 车站删除
     * @param id
     */
    public void delete(Long id){
        stationMapper.deleteByPrimaryKey(id);
    }

    /**
     * 查询所有车站
     * @param
     * @return
     */
    public List<StationQueryResp> queryAll(){
        StationExample stationExample = new StationExample();

        stationExample.setOrderByClause("id asc");
        stationExample.createCriteria();

        List<Station> stations = stationMapper.selectByExample(stationExample);

        return BeanUtil.copyToList(stations, StationQueryResp.class);
    }
}
