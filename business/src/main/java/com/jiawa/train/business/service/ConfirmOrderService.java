package com.jiawa.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jiawa.train.business.domain.*;
import com.jiawa.train.business.dto.ConfirmOrderMQDto;
import com.jiawa.train.business.enums.ConfirmOrderStatusEnum;
import com.jiawa.train.business.enums.RedisKeyPreEnum;
import com.jiawa.train.business.enums.SeatColEnum;
import com.jiawa.train.business.enums.SeatTypeEnum;
import com.jiawa.train.business.mapper.ConfirmOrderMapper;
import com.jiawa.train.business.req.ConfirmOrderDoReq;
import com.jiawa.train.business.req.ConfirmOrderQueryReq;
import com.jiawa.train.business.req.ConfirmOrderTicketReq;
import com.jiawa.train.business.resp.ConfirmOrderQueryResp;
import com.jiawa.train.exception.BusinessException;
import com.jiawa.train.exception.BusinessExceptionEnum;
import com.jiawa.train.resp.PageResp;
import com.jiawa.train.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ConfirmOrderService {
    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;
    @Resource
    private DailyTrainTicketService dailyTrainTicketService;
    @Resource
    private DailyTrainCarriageService dailyTrainCarriageService;
    @Resource
    private DailyTrainSeatService dailyTrainSeatService;
    @Resource
    private AfterConfirmOrderService afterConfirmOrderService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 1.新增乘客  2.修改乘客
     *
     * @param req
     */
    public void save(ConfirmOrderDoReq req) {

        DateTime now = DateTime.now();
        ConfirmOrder confirmOrder = BeanUtil.copyProperties(req, ConfirmOrder.class);
        if (ObjectUtil.isNull(confirmOrder.getId())) {
            confirmOrder.setId(SnowUtil.getSnowflakeNextId());
            confirmOrder.setCreateTime(now);
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.insert(confirmOrder);
        } else {
            confirmOrder.setUpdateTime(now);
//            confirmOrder.setCreateTime(req.getCreateTime());
            confirmOrderMapper.updateByPrimaryKey(confirmOrder);
        }
    }

    /**
     * 乘客查询 1.控制端查询所有乘客  2.business查询当前乘客
     *
     * @param req
     */
    public PageResp<ConfirmOrderQueryResp> queryList(ConfirmOrderQueryReq req) {
        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();

        confirmOrderExample.setOrderByClause("id desc");
        ConfirmOrderExample.Criteria criteria = confirmOrderExample.createCriteria();


        // 分页查询语句尽量与需要分页查询的sql语句放在一起，因为其只对最近的一条select语句生效
        LOG.info("页数：{}", req.getPage());
        LOG.info("每页大小：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<ConfirmOrder> confirmOrderList = confirmOrderMapper.selectByExample(confirmOrderExample);

        // 获取总数
        PageInfo<ConfirmOrder> confirmOrderPageInfo = new PageInfo<>(confirmOrderList);
        LOG.info("总行数：{}", confirmOrderPageInfo.getTotal());
        LOG.info("总页数：{}", confirmOrderPageInfo.getPages());

        List<ConfirmOrderQueryResp> confirmOrderQueryRespList = BeanUtil.copyToList(confirmOrderList, ConfirmOrderQueryResp.class);

        PageResp<ConfirmOrderQueryResp> objectPageResp = new PageResp<>();
        objectPageResp.setTotal(confirmOrderPageInfo.getTotal());
        objectPageResp.setList(confirmOrderQueryRespList);
        return objectPageResp;
    }

    /**
     * 乘客删除
     *
     * @param id
     */
    public void delete(Long id) {
        confirmOrderMapper.deleteByPrimaryKey(id);
    }

    @SentinelResource(value = "doConfirm", blockHandler = "doConfirmBlockHandler")
    public void doConfirm(ConfirmOrderMQDto dto) {
        String lockKey = RedisKeyPreEnum.CONFIRM_ORDER + "-" + dto.getDate() + "-" + dto.getTrainCode();

        // 令牌校验
//        boolean validSkToken = skTokenService.validSkToken(req.getDate(), req.getTrainCode(), LoginMemberContext.getMemberId());
//        if (validSkToken) {
//            LOG.info("令牌校验通过");
//        } else {
//            LOG.info("令牌校验不通过");
//            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_SK_TOKEN_FAIL);
//        }

        // 获取分布式锁
        Boolean setIfAbsent = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockKey, 5, TimeUnit.SECONDS);
        if (setIfAbsent) {
            LOG.info("恭喜抢到锁了! lockKey:{}", lockKey);
        } else {
            LOG.info("抢锁失败! lockKey:{}", lockKey);
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_LOCK_FAIL);
        }
//        RLock lock = null;
        try {
            /**
             * 循环：查出某一天，某个车次，状态为INIT的所有车次，为了减轻数据库的压力，采用分页查询，不把数据所有数据一次性查出来放到内存中
             * 1.查询余票详情表，根据车次，日期，座位类型，座位号，座位状态，座位状态为INIT的
             * 2.构建分页
             * 3.查询
             * 4.对查询结果作非空判断，如果为空，跳出循环
             * 5.对查询结果（列表）循环调用sell
             */
            while (true) {
                ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
                confirmOrderExample.setOrderByClause("id asc");
                confirmOrderExample.createCriteria()
                        .andDateEqualTo(dto.getDate())
                        .andTrainCodeEqualTo(dto.getTrainCode())
                        .andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());
                PageHelper.startPage(1, 5);
                List<ConfirmOrder> confirmOrders = confirmOrderMapper.selectByExampleWithBLOBs(confirmOrderExample);
                if (CollUtil.isEmpty(confirmOrders)) {
                    LOG.info("没有需要处理的票了！");
                    break;
                } else {
                    LOG.info("需要处理的票数：{}", confirmOrders.size());
                }
                for (ConfirmOrder confirmOrder : confirmOrders) {
                    sell(confirmOrder);
                }
            }
        } finally {
//            LOG.info("购票流程结束，释放锁！");
//            if (null != lock && lock.isHeldByCurrentThread()) {
//                lock.unlock();
//            }
            LOG.info("选座结束,删除分布式锁");
            stringRedisTemplate.delete(lockKey);
        }
    }

    private void sell(ConfirmOrder confirmOrder) {
        //            // 获取锁
//            lock = redissonClient.getLock(lockKey);
//            /**
//             waitTime – the maximum time to acquire the lock 等待获取锁时间(最大尝试获得锁的时间)，超时返回false
//             leaseTime – lease time 锁时长，即n秒后自动释放锁
//             time unit – time unit 时间单位
//             */
//            // boolean tryLock = lock.tryLock(30, 10, TimeUnit.SECONDS); // 不带看门狗
//            // 尝试上锁
//            boolean tryLock = lock.tryLock(0, TimeUnit.SECONDS); // 带看门狗
//            if (tryLock) {
//                LOG.info("恭喜，抢到锁了！");
//                // 可以把下面这段放开，只用一个线程来测试，看看redisson的看门狗效果
//                // for (int i = 0; i < 30; i++) {
//                //     Long expire = redisTemplate.opsForValue().getOperations().getExpire(lockKey);
//                //     LOG.info("锁过期时间还有：{}", expire);
//                //     Thread.sleep(1000);
//                // }
//            } else {
//                // 只是没抢到锁，并不知道票抢完了没，所以提示稍候再试
//                LOG.info("很遗憾，没抢到锁");
//                throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_LOCK_FAIL);
//            }

        DateTime now = DateTime.now();
        // 省略数据校验（req中数据合法性校验）， 业务校验，比如今天不能买昨天的票，同乘客同车次不同重复买票
        String start = confirmOrder.getStart();
        String end = confirmOrder.getEnd();
        Date date = confirmOrder.getDate();
        String trainCode = confirmOrder.getTrainCode();


        // 构建ConfirmOrderDoReq
        ConfirmOrderDoReq req = new ConfirmOrderDoReq();
        req.setMemberId(confirmOrder.getMemberId());
        req.setDate(date);
        req.setTrainCode(trainCode);
        req.setStart(start);
        req.setEnd(end);
        req.setDailyTrainTicketId(confirmOrder.getDailyTrainTicketId());
        req.setTickets(JSON.parseArray(confirmOrder.getTickets(), ConfirmOrderTicketReq.class));

        // 将订单设置成处理中，避免重复处理
        LOG.info("将确认订单更新成处理中，避免重复处理，confirm_order.id: {}", confirmOrder.getId());
        confirmOrder.setStatus(ConfirmOrderStatusEnum.PENDING.getCode());
        updateStatus(confirmOrder);
        // 保存确认订单，状态初始
        // 改为从数据库中查询
//        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
//        confirmOrderExample.setOrderByClause("id asc");
//        confirmOrderExample.createCriteria()
//                .andDateEqualTo(date)
//                .andTrainCodeEqualTo(trainCode)
//                .andMemberIdEqualTo(memberId)
//                .andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());
//        List<ConfirmOrder> confirmOrders = confirmOrderMapper.selectByExampleWithBLOBs(confirmOrderExample);
//        ConfirmOrder confirmOrder = null;
//        if (CollUtil.isNotEmpty(confirmOrders)) {
//            LOG.info("本次处理{}条订单", confirmOrders.size());
//            confirmOrder = confirmOrders.get(0);
//        } else {
//            LOG.info("找不到原始订单，结束");
//            return;
//        }

        // 查询余票记录，得到真实的库存
        DailyTrainTicket dailyTrainTicket = dailyTrainTicketService.selectByUnique(date, trainCode, start, end);
        LOG.info("余票记录：{}", dailyTrainTicket);
        // 扣减余票库存，用于判断余票是否足够
        reduceTicket(req, dailyTrainTicket);
        List<ConfirmOrderTicketReq> confirmOrderTicketReqList = req.getTickets();

        // 计算相对于选定的第一个座位的偏移值（可以选座时）
        // 根据第一张票的座位类型是否为空，判断能否选座
        ConfirmOrderTicketReq ticketReq = confirmOrderTicketReqList.get(0);

        // 定义一个变量，表示最终选座结果
        List<DailyTrainSeat> finalSeatList = new ArrayList<>();

        if (StrUtil.isNotBlank(ticketReq.getSeat())) {
            // 可选座时
            LOG.info("可选座");
            // 查出这种座位类型有多少列（不同座位类型，偏移值不同）
            List<SeatColEnum> colEnumList = SeatColEnum.getColsByType(ticketReq.getSeatTypeCode());
            LOG.info("座位类型包含的列：{}", colEnumList);
            // 组成和前端相同的两排座位（循环）
            List<String> referSeatList = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                for (SeatColEnum seatColEnum : colEnumList) {
                    referSeatList.add((seatColEnum.getCode() + (i + 1)));
                }
            }
            LOG.info("参考座位：{}", referSeatList);


            // 计算偏移值
            // 1. 绝对偏移值，遍历座位号，获得每一张票的seat
            List<Integer> absOffsetList = new ArrayList<>();
            for (ConfirmOrderTicketReq ticketReq1 : confirmOrderTicketReqList) {
                absOffsetList.add(referSeatList.indexOf(ticketReq1.getSeat()));
            }
            LOG.info("绝对偏移值：{}", absOffsetList);

            // 2. 计算相对偏移值（都减去第一个座位的绝对偏移值）
            List<Integer> relOffsetList = new ArrayList<>();
            for (Integer index : absOffsetList) {
                relOffsetList.add(index - absOffsetList.get(0));
            }
            LOG.info("相对偏移值：{}", relOffsetList);

            // 进行选座
            getSeat(finalSeatList,
                    date,
                    trainCode,
                    ticketReq.getSeatTypeCode(),
                    ticketReq.getSeat().split("")[0],
                    relOffsetList,
                    dailyTrainTicket.getStartIndex(),
                    dailyTrainTicket.getEndIndex());
        } else {
            // 不可选座时
            LOG.info("不可选座");

                    /*进行选座：
                        循环每张票，获取这张票的座位类型*/
            for (ConfirmOrderTicketReq confirmOrderTicketReq : confirmOrderTicketReqList) {
                getSeat(finalSeatList,
                        date,
                        trainCode,
                        confirmOrderTicketReq.getSeatTypeCode(),
                        null,
                        null,
                        dailyTrainTicket.getStartIndex(),
                        dailyTrainTicket.getEndIndex());
            }
        }

        LOG.info("最终选座结果：{}", finalSeatList);

        // 选中座位后事务处理
        //     座位表售卖情况修改
        //     余票详情表修改余票
        //     为会员增加购票记录
        //     更新确认订单表
        try {
            afterConfirmOrderService.afterDoConfirm(dailyTrainTicket, finalSeatList, confirmOrderTicketReqList, confirmOrder);
        } catch (Exception e) {
            LOG.error("保存购票信息失败", e);
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_EXCEPTION);
        }
//        } catch (InterruptedException e) {
//            LOG.error("购票异常", e);
    }

    /**
     * 一个循环挑选所有购票的座位
     *
     * @param date
     * @param trainCode
     * @param seatType
     * @param column
     * @param relOffsetList
     */
    private void getSeat(List<DailyTrainSeat> finalSeatList, Date date, String trainCode, String seatType, String column, List<Integer> relOffsetList, Integer startIndex, Integer endIndex) {
            /*遍历车厢获取座位数据：
                获取所有车厢；
                遍历每个车厢，获取这个车厢下的座位数据，设置座位；*/
        // 选座过程中，将可能的选座结果保存到这个变量中
        List<DailyTrainSeat> getSeatList;

        // 根据座位类型查询对应的车厢
        List<DailyTrainCarriage> dailyTrainCarriages = dailyTrainCarriageService.selectBySeatType(date, trainCode, seatType);
        LOG.info("车厢总数{}", dailyTrainCarriages.size());

        // 遍历符合购票信息中座位类型的车厢
        for (DailyTrainCarriage dailyTrainCarriage : dailyTrainCarriages) {
            LOG.info("开始从车厢{}选座", dailyTrainCarriage.getIndex());

            // 在新的车厢开始选座时，清空getSeatList
            getSeatList = new ArrayList<>();

            // 获取这个车厢的所有座位
            List<DailyTrainSeat> dailyTrainSeats = dailyTrainSeatService.selectByCarriage(date, trainCode, dailyTrainCarriage.getIndex());
            LOG.info("车厢{}座位总数{}", dailyTrainCarriage.getIndex(), dailyTrainSeats.size());

            // 遍历这个车厢的所有座位
            for (DailyTrainSeat dailyTrainSeat : dailyTrainSeats) {
                Integer seatIndex = dailyTrainSeat.getCarriageSeatIndex();
//                在遍历座位的过程中，判断当前座位的col，是否与购票信息中选中的座位column相同；
//                1. 获取购票信息中的座位列号，判断是否为空：
                String col = dailyTrainSeat.getCol();

//                在挑选座位时，还应该到一个暂存的结果中查找，看这个座位有没有在这一次的选座中被选中；
                boolean alreadyChoose = false;
                for (DailyTrainSeat finalSeat : finalSeatList) {
                    if (finalSeat.getId().equals(dailyTrainSeat.getId())) {
                        alreadyChoose = true;
                        break;
                    }
                }
                if (alreadyChoose) {
                    LOG.info("座位{}已选中，跳过", seatIndex);
                    continue;
                }

                if (StrUtil.isBlank(column)) {
//                    1.1 空，表示没有选座
                    LOG.info("没有选座");
                } else {
//                    1.2 不空，表示有选座
                    LOG.info("有选座");
//                    将当前遍历到的座位的col，与购票信息中的座位的col比对
                    if (!column.equals(col)) {
//                        继续判断下一个座位
                        LOG.info("座位{}的列号{}与购票信息中的列号{}不同，继续下一个座位",
                                seatIndex, col, column);
                        continue;
                    }
                }

                boolean isChoose = calSell(dailyTrainSeat, startIndex, endIndex);
                /**
                 * 如果已选中，结束这个方法
                 * 如果未选中，continue
                 */
                if (isChoose) {
//                    选中了第一个座位，放入临时变量中
                    getSeatList.add(dailyTrainSeat);
                    LOG.info("选中座位");
                } else {
                    continue;
                }

//                        保证所有座位在同一个车厢，根据nextIndex和座位总数之间的关系判断
//                        if 不成立，则跳过当前车厢，设置变量
                boolean isGetAllOffsetSeat = true;

//                已经选完了第一个座位，根据relOffsetList，选择剩下的座位
//                首先判断relOffsetList是否为空，不为空，表示有进行选座
                if (CollUtil.isNotEmpty(relOffsetList)) {
                    LOG.info("有偏移值{}，校验偏移的座位是否可选", relOffsetList);
//                    遍历relOffsetList，从第二个开始计算剩余的座位

                    for (int i = 1; i < relOffsetList.size(); i++) {
//                    获取偏移值，并计算得到剩余的座位
//                        保存可能的选座结果，最后赋值给最终的选座结果
                        Integer offset = relOffsetList.get(i);
                        int nextIndex = seatIndex + offset - 1;

//                        当前座位编号已经大于这节车厢中座位编号的上限，这个座位不在当前车厢
                        if (nextIndex > dailyTrainSeats.size()) {
                            LOG.info("座位{}不在同一车厢，跳过", nextIndex);
                            isGetAllOffsetSeat = false;
                            break;
                        }

                        DailyTrainSeat nextDailyTrainSeat = dailyTrainSeats.get(nextIndex);
                        // 判断这个座位是否可选
                        boolean isChooseNext = calSell(nextDailyTrainSeat, startIndex, endIndex);
                        if (isChooseNext) {
//                            选中了购票信息中下一个座位
                            getSeatList.add(nextDailyTrainSeat);
                            LOG.info("座位{}已选中", nextDailyTrainSeat.getCarriageSeatIndex());
                        } else {
                            LOG.info("座位{}未选中", nextDailyTrainSeat.getCarriageSeatIndex());
                            isGetAllOffsetSeat = false;
                            break;
                        }
                    }
                }
                if (!isGetAllOffsetSeat) {
//                  假如没有选中全部的座位，需要清除getSeatList中的信息，重新从第一个座位开始选座
                    getSeatList = new ArrayList<>();
                    continue;
                }

//                全部座位已经选好，保存座位
//                目前问题：没有将结果保存，连续两次选中了同一个座位；
                finalSeatList.addAll(getSeatList);
                return;
            }
        }
    }

    /**
     * 更新状态
     * @param confirmOrder
     */
    public void updateStatus(ConfirmOrder confirmOrder) {
        ConfirmOrder confirmOrderForUpdate = new ConfirmOrder();
        confirmOrderForUpdate.setId(confirmOrder.getId());
        confirmOrderForUpdate.setUpdateTime(new Date());
        confirmOrderForUpdate.setStatus(confirmOrder.getStatus());
        confirmOrderMapper.updateByPrimaryKeySelective(confirmOrderForUpdate);
    }

    /**
     * 计算某个座位在某一区间是否可卖
     */
    private boolean calSell(DailyTrainSeat dailyTrainSeat, Integer startIndex, Integer endIndex) {
        /*sell=10001，本次购买区间1~4，这一区间已售000，
        只要这个有1，表明这个区间已售；

        选中后，计算购买后的sell，假如本次购买1~4，则为01110，
        与原来按位做或运算*/

//        得到当前座位的所有站购买信息
        String sell = dailyTrainSeat.getSell();
//        截取上述信息中，当前购票的start和end之间的售卖情况
        String sellPart = sell.substring(startIndex, endIndex);
//        转化为整数，if 大于0，说明不可选
        if (Integer.parseInt(sellPart) > 0) {
            LOG.info("座位{}在本车次车站区间{}~{}已售", dailyTrainSeat.getCarriageSeatIndex(), startIndex, endIndex);
            return false;
        } else {
            //            转化为整数，if 等于0，说明可售
            LOG.info("座位{}在本车次车站区间{}~{}未售", dailyTrainSeat.getCarriageSeatIndex(), startIndex, endIndex);
//            构造本次的售卖信息，将原来的0替换为1
            String curSell = sellPart.replace('0', '1');
//            在两边添加0，0的表示剩下的站， 左边补0（endIndex）， 右边补0（全部站的长度）
            curSell = StrUtil.fillBefore(curSell, '0', endIndex);
            curSell = StrUtil.fillAfter(curSell, '0', sell.length());
//            将当前区间的售票信息和数据库中已售信息按位与运算
            int newSellInt = NumberUtil.binaryToInt(curSell) | NumberUtil.binaryToInt(sell);
//            转换为二进制字符串，考虑高位可能为0，转换时会丢弃，因此需要重新补0
            String newSell = NumberUtil.getBinaryStr(newSellInt);
            newSell = StrUtil.fillBefore(newSell, '0', sell.length());
            LOG.info("座位{}被选中，原售卖信息:{}，本车次车站区间{}~{}，即:{}，最终售票信息:{}",
                    dailyTrainSeat.getCarriageSeatIndex(), sell, startIndex, endIndex, curSell, newSell);
//            修改临时变量
            dailyTrainSeat.setSell(newSell);
            return true;
        }
    }

    // 扣减余票库存
    private void reduceTicket(ConfirmOrderDoReq req, DailyTrainTicket
            dailyTrainTicket) {
        for (ConfirmOrderTicketReq ticketReq : req.getTickets()) {
            String seatTypeCode = ticketReq.getSeatTypeCode();
            SeatTypeEnum seatTypeEnum = EnumUtil.getBy(SeatTypeEnum::getCode, seatTypeCode);
            switch (seatTypeEnum) {
                case YDZ -> {
                    int countYDZLeft = dailyTrainTicket.getYdz() - 1;
                    if (countYDZLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.BUSSINESS_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYdz(countYDZLeft);
                }
                case EDZ -> {
                    int countEDZLeft = dailyTrainTicket.getEdz() - 1;
                    if (countEDZLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.BUSSINESS_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setEdz(countEDZLeft);
                }
                case RW -> {
                    int countRWLeft = dailyTrainTicket.getRw() - 1;
                    if (countRWLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.BUSSINESS_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setRw(countRWLeft);
                }
                case YW -> {
                    int countYWLeft = dailyTrainTicket.getYw() - 1;
                    if (countYWLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.BUSSINESS_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYw(countYWLeft);
                }
            }
        }
    }

    public void doConfirmBlockHandler(ConfirmOrderDoReq req, BlockException e) {
        LOG.info("购票请求被限流");
        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
    }
}
