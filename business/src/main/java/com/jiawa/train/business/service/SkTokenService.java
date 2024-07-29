package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.SkToken;
import com.jiawa.train.business.domain.SkTokenExample;
import com.jiawa.train.business.mapper.SkTokenMapper;
import com.jiawa.train.business.req.SkTokenQueryReq;
import com.jiawa.train.business.req.SkTokenSaveReq;
import com.jiawa.train.business.resp.SkTokenQueryResp;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class SkTokenService {
    private static final Logger LOG = LoggerFactory.getLogger(SkTokenService.class);

    @Resource
    private SkTokenMapper skTokenMapper;
    @Resource
    private DailyTrainSeatService dailyTrainSeatService;
    @Resource
    private DailyTrainStationService dailyTrainStationService;

    /**
     * 1.新增乘客  2.修改乘客
     *
     * @param req
     */
    public void save(SkTokenSaveReq req) {
        DateTime now = DateTime.now();
        SkToken skToken = BeanUtil.copyProperties(req, SkToken.class);
        if (ObjectUtil.isNull(skToken.getId())) {
            skToken.setId(SnowUtil.getSnowflakeNextId());
            skToken.setCreateTime(now);
            skToken.setUpdateTime(now);
            skTokenMapper.insert(skToken);
        } else {
            skToken.setUpdateTime(now);
            skToken.setCreateTime(req.getCreateTime());
            skTokenMapper.updateByPrimaryKey(skToken);
        }
    }

    /**
     * 乘客查询 1.控制端查询所有乘客  2.business查询当前乘客
     *
     * @param req
     */
    public PageResp<SkTokenQueryResp> queryList(SkTokenQueryReq req) {
        SkTokenExample skTokenExample = new SkTokenExample();

        skTokenExample.setOrderByClause("id desc");
        SkTokenExample.Criteria criteria = skTokenExample.createCriteria();


        // 分页查询语句尽量与需要分页查询的sql语句放在一起，因为其只对最近的一条select语句生效
        LOG.info("页数：{}", req.getPage());
        LOG.info("每页大小：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<SkToken> skTokenList = skTokenMapper.selectByExample(skTokenExample);

        // 获取总数
        PageInfo<SkToken> skTokenPageInfo = new PageInfo<>(skTokenList);
        LOG.info("总行数：{}", skTokenPageInfo.getTotal());
        LOG.info("总页数：{}", skTokenPageInfo.getPages());

        List<SkTokenQueryResp> skTokenQueryRespList = BeanUtil.copyToList(skTokenList, SkTokenQueryResp.class);

        PageResp<SkTokenQueryResp> objectPageResp = new PageResp<>();
        objectPageResp.setTotal(skTokenPageInfo.getTotal());
        objectPageResp.setList(skTokenQueryRespList);
        return objectPageResp;
    }

    /**
     * 乘客删除
     *
     * @param id
     */
    public void delete(Long id) {
        skTokenMapper.deleteByPrimaryKey(id);
    }

    public void genDaily(Date date, String trainCode) {
        // 打印日志
        LOG.info("开始生成日期【{}】车次【{}】的令牌记录", DateUtil.formatDate(date), trainCode);
        // 先删除后生成
        // 查出traincode对应的基础车次车站信息

        // 考虑重复生成
        // 首先将数据库中对应车次数据清空，日期和车次
        SkTokenExample skTokenExample = new SkTokenExample();
        skTokenExample.createCriteria()
                .andTrainCodeEqualTo(trainCode)
                .andDateEqualTo(date);
        // 执行删除操作
        skTokenMapper.deleteByExample(skTokenExample);


        // 由于一趟车次有只设置一个令牌
        // 生成每天的站点信息
        // 生成date的编号为code的车次
        DateTime now = DateTime.now();
        SkToken skToken = new SkToken();
        // 设置skToken的id,date,createTime,updateTime
        skToken.setId(SnowUtil.getSnowflakeNextId());
        skToken.setDate(date);
        skToken.setCreateTime(now);
        skToken.setUpdateTime(now);
        skToken.setTrainCode(trainCode);

        // 生成令牌总数：一趟车的座位数*一趟车的站点数*3/4
        int countSeat = dailyTrainSeatService.countSeat(date, trainCode);
        LOG.info("车次【{}】的座位数：{}", trainCode, countSeat);

        int countStation = dailyTrainStationService.countStation(date, trainCode);
        LOG.info("车次【{}】的站数：{}", trainCode, countStation);

        int count = (int) (countSeat * countStation * 3 / 4);
        LOG.info("车次【{}】的令牌总数：{}", trainCode, countStation);
        skToken.setCount(count);

        // 插入数据库
        skTokenMapper.insert(skToken);
        // 打印日志
        LOG.info("生成日期【{}】车次【{}】的令牌记录结束", DateUtil.formatDate(date), trainCode);

    }
}
