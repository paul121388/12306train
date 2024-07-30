package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.SkToken;
import com.jiawa.train.business.domain.SkTokenExample;
import com.jiawa.train.business.enums.RedisKeyPreEnum;
import com.jiawa.train.business.mapper.SkTokenMapper;
import com.jiawa.train.business.mapper.customer.SkTokenMapperCust;
import com.jiawa.train.business.req.SkTokenQueryReq;
import com.jiawa.train.business.req.SkTokenSaveReq;
import com.jiawa.train.business.resp.SkTokenQueryResp;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SkTokenService {
    private static final Logger LOG = LoggerFactory.getLogger(SkTokenService.class);

    @Resource
    private SkTokenMapper skTokenMapper;
    @Resource
    private DailyTrainSeatService dailyTrainSeatService;
    @Resource
    private DailyTrainStationService dailyTrainStationService;
    @Resource
    private SkTokenMapperCust skTokenMapperCust;
    @Resource
    private RedisTemplate redisTemplate;


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

        int count = (int) (countSeat * countStation);
        LOG.info("车次【{}】的令牌总数：{}", trainCode, countStation);
        skToken.setCount(count);

        // 插入数据库
        skTokenMapper.insert(skToken);
        // 打印日志
        LOG.info("生成日期【{}】车次【{}】的令牌记录结束", DateUtil.formatDate(date), trainCode);

    }

    /**
     * 校验令牌
     */
    public boolean validSkToken(Date date, String trainCode, Long memberId) {
        LOG.info("会员【{}】获取日期【{}】车次【{}】的令牌开始", memberId, DateUtil.formatDate(date), trainCode);

        // 需要去掉这段，否则发布生产后，体验多人排队功能时，会因拿不到锁而返回：等待5秒，加入20人时，只有第1次循环能拿到锁
        // if (!env.equals("dev")) {
             // 先获取令牌锁，再校验令牌余量，防止机器人抢票，lockKey就是令牌，用来表示【谁能做什么】的一个凭证
             String lockKey = RedisKeyPreEnum.SK_TOKEN +"-" + DateUtil.formatDate(date) + "-" + trainCode + "-" + memberId; //
             Boolean setIfAbsent = redisTemplate.opsForValue().setIfAbsent(lockKey, lockKey, 5, TimeUnit.SECONDS);
             if (Boolean.TRUE.equals(setIfAbsent)) {
                 LOG.info("恭喜，抢到令牌锁了！lockKey：{}", lockKey);
             } else {
                 LOG.info("很遗憾，没抢到令牌锁！lockKey：{}", lockKey);
                 return false;
             }
        // }

        /**
         * 获取当前车次的令牌数量的Key： 枚举值（防止不用的作业重复）+日期+车次
         * 从缓存中获取当前车次令牌数量（根据Key）
         *
         * 如果缓存中存在当前车次令牌数量
         *   将缓存中的令牌数量-1，用Long类型变量接受-1后的结果
         *   如果扣减后令牌数量小于0，则说明没有令牌了
         *      return false
         *   否则
         *      延长令牌数量在缓存中的有效期
         *      当缓存数量为5的倍数时
         *        更新数据库中对应车次令牌数量（目的：减少访问数据库的次数）
         *
         * 如果缓存中不存在当前车次令牌数量
         *    查询数据库中对应车次令牌数量
         *
         *    对从数据库中查询到的数据进行判断
         *    如果为空
         *      return false
         *
         *    （正常来说一列车次最多只有一条记录）获取数据库中查询的列表的第一个元素
         *
         *    如果令牌数量小于0
         *      将令牌数量设置为0
         *      return false
         *
         *    将键值对放入缓存中
         */
        String skTokenCountKey = RedisKeyPreEnum.SK_TOKEN_COUNT + "-" + DateUtil.formatDate(date) + "-" + trainCode;
        Object skTokenCount = redisTemplate.opsForValue().get(skTokenCountKey);
        if (skTokenCount != null) {
            LOG.info("缓存中有该车次令牌大闸的key：{}", skTokenCountKey);
            Long count = redisTemplate.opsForValue().decrement(skTokenCountKey, 1);
            if (count < 0L) {
                LOG.error("获取令牌失败：{}", skTokenCountKey);
                return false;
            } else {
                LOG.info("获取令牌后，令牌余数：{}", count);
                redisTemplate.expire(skTokenCountKey, 60, TimeUnit.SECONDS);
                // 每获取5个令牌更新一次数据库
                if (count % 5 == 0) {
                    skTokenMapperCust.decrease(date, trainCode, 5);
                }
                return true;
            }
        } else {
            LOG.info("缓存中没有该车次令牌大闸的key：{}", skTokenCountKey);
            // 检查是否还有令牌
            SkTokenExample skTokenExample = new SkTokenExample();
            skTokenExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
            List<SkToken> tokenCountList = skTokenMapper.selectByExample(skTokenExample);
            if (CollUtil.isEmpty(tokenCountList)) {
                LOG.info("找不到日期【{}】车次【{}】的令牌记录", DateUtil.formatDate(date), trainCode);
                return false;
            }

            SkToken skToken = tokenCountList.get(0);
            if (skToken.getCount() <= 0) {
                LOG.info("日期【{}】车次【{}】的令牌余量为0", DateUtil.formatDate(date), trainCode);
                return false;
            }

            // 令牌还有余量
            // 令牌余数-1
            Integer count = skToken.getCount() - 1;
            skToken.setCount(count);
            LOG.info("将该车次令牌大闸放入缓存中，key: {}， count: {}", skTokenCountKey, count);
            // 不需要更新数据库，只要放缓存即可
            redisTemplate.opsForValue().set(skTokenCountKey, String.valueOf(count), 60, TimeUnit.SECONDS);
            // skTokenMapper.updateByPrimaryKey(skToken);
            return true;
        }

        // 令牌约等于库存，令牌没有了，就不再卖票，不需要再进入购票主流程去判断库存，判断令牌肯定比判断库存效率高
//         int updateCount = skTokenMapperCust.decrease(date, trainCode, 1);
//         if (updateCount > 0) {
//             return true;
//         } else {
//             return false;
//         }
    }
}
