package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.TrainCarriage;
import com.jiawa.train.business.domain.TrainSeat;
import com.jiawa.train.business.domain.TrainSeatExample;
import com.jiawa.train.business.enums.SeatColEnum;
import com.jiawa.train.business.mapper.TrainSeatMapper;
import com.jiawa.train.business.req.TrainSeatQueryReq;
import com.jiawa.train.business.req.TrainSeatSaveReq;
import com.jiawa.train.business.resp.TrainSeatQueryResp;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainSeatService {
    private static final Logger LOG = LoggerFactory.getLogger(TrainSeatService.class);

    @Resource
    private TrainSeatMapper trainSeatMapper;

    @Resource
    private TrainCarriageService trainCarriageService;

    /**
     * 1.新增座位  2.修改座位
     *
     * @param req
     */
    public void save(TrainSeatSaveReq req) {
        DateTime now = DateTime.now();
        TrainSeat trainSeat = BeanUtil.copyProperties(req, TrainSeat.class);
        if (ObjectUtil.isNull(trainSeat.getId())) {
            trainSeat.setId(SnowUtil.getSnowflakeNextId());
            trainSeat.setCreateTime(now);
            trainSeat.setUpdateTime(now);
            trainSeatMapper.insert(trainSeat);
        } else {
            trainSeat.setUpdateTime(now);
            trainSeat.setCreateTime(req.getCreateTime());
            trainSeatMapper.updateByPrimaryKey(trainSeat);
        }
    }

    /**
     * 座位查询 1.控制端查询所有座位  2.business查询当前座位
     *
     * @param req
     */
    public PageResp<TrainSeatQueryResp> queryList(TrainSeatQueryReq req) {
        TrainSeatExample trainSeatExample = new TrainSeatExample();

        trainSeatExample.setOrderByClause("carriage_index, id asc");
        TrainSeatExample.Criteria criteria = trainSeatExample.createCriteria();

        if(StrUtil.isNotBlank(req.getTrainCode())) {
            criteria.andTrainCodeEqualTo(req.getTrainCode());
        }

        // 分页查询语句尽量与需要分页查询的sql语句放在一起，因为其只对最近的一条select语句生效
        LOG.info("页数：{}", req.getPage());
        LOG.info("每页大小：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<TrainSeat> trainSeatList = trainSeatMapper.selectByExample(trainSeatExample);

        // 获取总数
        PageInfo<TrainSeat> trainSeatPageInfo = new PageInfo<>(trainSeatList);
        LOG.info("总行数：{}", trainSeatPageInfo.getTotal());
        LOG.info("总页数：{}", trainSeatPageInfo.getPages());

        List<TrainSeatQueryResp> trainSeatQueryRespList = BeanUtil.copyToList(trainSeatList, TrainSeatQueryResp.class);

        PageResp<TrainSeatQueryResp> objectPageResp = new PageResp<>();
        objectPageResp.setTotal(trainSeatPageInfo.getTotal());
        objectPageResp.setList(trainSeatQueryRespList);
        return objectPageResp;
    }

    /**
     * 座位删除
     *
     * @param id
     */
    public void delete(Long id) {
        trainSeatMapper.deleteByPrimaryKey(id);
    }

    /**
     * 生成当前火车座位
     *
     * @param trainCode
     */
    public void genTrainSeat(String trainCode) {
        DateTime now = DateTime.now();
        // 清空当前车次下的所有座位记录
        TrainSeatExample trainSeatExample = new TrainSeatExample();
        TrainSeatExample.Criteria criteria = trainSeatExample.createCriteria();
        criteria.andTrainCodeEqualTo(trainCode);
        LOG.info("车次：{}，删除座位记录", trainCode);
        trainSeatMapper.deleteByExample(trainSeatExample);

        // 查找当前车次下的所有车厢
        List<TrainCarriage> trainCarriages = trainCarriageService.selectByTrainCode(trainCode);
        LOG.info("车次：{}，车厢数量：{}", trainCode, trainCarriages.size());
        // 循环生成每个车厢的座位
        for (TrainCarriage trainCarriage : trainCarriages) {
            // 拿到车厢的数据：行数，座位类型（根据座位类型得到列数）
            Integer rowCount = trainCarriage.getRowCount();
            String seatType = trainCarriage.getSeatType();
            LOG.info("车厢：{}，座位类型：{}, 行数：{}", trainCarriage.getIndex(), seatType, rowCount);
            int seatIndex = 1;

            // 根据车厢的座位类型，筛选出所有的列，比如车厢类型是一等座，那么列数就是4
            List<SeatColEnum> colEnumList = SeatColEnum.getColsByType(seatType);
            // 循环行数
            for (int row = 1; row <= rowCount; row++) {
                // 循环列数
                for (SeatColEnum col : colEnumList) {
                    // 构造座位数据并保存数据库
                    TrainSeat trainSeat = new TrainSeat();
                    trainSeat.setId(SnowUtil.getSnowflakeNextId());
                    trainSeat.setTrainCode(trainCode);
                    trainSeat.setCarriageIndex(trainCarriage.getIndex());
                    trainSeat.setRow(StrUtil.fillBefore(String.valueOf(row), '0', 2));
                    trainSeat.setCol(col.getCode());
                    trainSeat.setSeatType(seatType);
                    trainSeat.setCarriageSeatIndex(seatIndex++);
                    trainSeat.setCreateTime(now);
                    trainSeat.setUpdateTime(now);

                    LOG.info("生成座位数据车次：{}，车厢：{}, 行：{}, 列：{}, 座位类型：{}",
                            trainCode, trainCarriage.getIndex(), row, col.getCode(),seatType);
                    trainSeatMapper.insert(trainSeat);
                }
            }
        }
    }

    /**
     * 根据列车code，查询列车所属的所有车厢
     * @param trainCode
     * @return
     */
    public List<TrainSeat> selectByTrainCode(String trainCode) {
        TrainSeatExample example = new TrainSeatExample();
        example.createCriteria().andTrainCodeEqualTo(trainCode);
        example.setOrderByClause("id asc");
        return trainSeatMapper.selectByExample(example);
    }
}
