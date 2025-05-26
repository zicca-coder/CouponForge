package com.example.coupon.engine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.coupon.engine.common.constant.EngineRedisConstant;
import com.example.coupon.engine.common.context.UserContext;
import com.example.coupon.engine.dao.entity.CouponTemplateRemindDO;
import com.example.coupon.engine.dao.mapper.CouponTemplateRemindMapper;
import com.example.coupon.engine.dto.req.CouponTemplateQueryReqDTO;
import com.example.coupon.engine.dto.req.CouponTemplateRemindCancelReqDTO;
import com.example.coupon.engine.dto.req.CouponTemplateRemindCreateReqDTO;
import com.example.coupon.engine.dto.req.CouponTemplateRemindQueryReqDTO;
import com.example.coupon.engine.dto.resp.CouponTemplateQueryRespDTO;
import com.example.coupon.engine.dto.resp.CouponTemplateRemindQueryRespDTO;
import com.example.coupon.engine.mq.event.CouponTemplateRemindDelayEvent;
import com.example.coupon.engine.mq.producer.CouponTemplateRemindDelayProducer;
import com.example.coupon.engine.service.CouponTemplateRemindService;
import com.example.coupon.engine.service.CouponTemplateService;
import com.example.coupon.engine.service.handler.remind.dto.CouponTemplateRemindDTO;
import com.example.coupon.engine.toolkit.CouponTemplateRemindUtil;
import com.example.coupon.framework.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 优惠券预约提醒业务逻辑实现层
 */
@Service
@RequiredArgsConstructor
public class CouponTemplateRemindServiceImpl extends ServiceImpl<CouponTemplateRemindMapper, CouponTemplateRemindDO> implements CouponTemplateRemindService {

    private final CouponTemplateRemindMapper couponTemplateRemindMapper;
    private final CouponTemplateService couponTemplateService;
    private final RBloomFilter<String> cancelRemindBloomFilter;
    private final CouponTemplateRemindDelayProducer couponTemplateRemindDelayProducer;
    private final StringRedisTemplate stringRedisTemplate;


    /**
     * 用户创建预约提醒
     * @param requestParam 请求参数
     */
    @Override
    public void createCouponRemind(CouponTemplateRemindCreateReqDTO requestParam) {
        // 验证优惠券是否存在，避免缓存穿透问题并获取优惠券开抢时间
        CouponTemplateQueryRespDTO couponTemplate = couponTemplateService
                .findCouponTemplate(new CouponTemplateQueryReqDTO(requestParam.getShopNumber(), requestParam.getCouponTemplateId()));
        if (couponTemplate.getValidStartTime().before(new Date())) {
            throw new ClientException("无法预约已开始领取的优惠券");
        }
        CouponTemplateRemindDO couponTemplateRemindDO = lambdaQuery().eq(CouponTemplateRemindDO::getUserId, UserContext.getUserId())
                .eq(CouponTemplateRemindDO::getCouponTemplateId, requestParam.getCouponTemplateId())
                .one();
        // 如果没创建过提醒
        if (couponTemplateRemindDO == null) {
            couponTemplateRemindDO = BeanUtil.toBean(requestParam, CouponTemplateRemindDO.class);

            // 设置优惠券开抢时间信息
            couponTemplateRemindDO.setStartTime(couponTemplate.getValidStartTime());
            couponTemplateRemindDO.setUserId(Long.parseLong(UserContext.getUserId()));
            couponTemplateRemindDO.setInformation(CouponTemplateRemindUtil.calculateBitMap(requestParam.getRemindTime(), requestParam.getType()));

            // 插入预约提醒记录
            couponTemplateRemindMapper.insert(couponTemplateRemindDO);
        } else {
            Long information = couponTemplateRemindDO.getInformation();
            Long bitMap = CouponTemplateRemindUtil.calculateBitMap(requestParam.getRemindTime(), requestParam.getType());
            if ((information & bitMap) != 0L) {
                throw new ClientException("已经创建过该提醒了");
            }
            // 位图 逻辑与 运算叠加预约信息
            couponTemplateRemindDO.setInformation(information ^ bitMap);

            lambdaUpdate().eq(CouponTemplateRemindDO::getUserId, UserContext.getUserId())
                    .eq(CouponTemplateRemindDO::getCouponTemplateId, requestParam.getCouponTemplateId())
                    .update(couponTemplateRemindDO);
        }

        // 用户预约记录添加到数据库后，发送预约提醒抢购优惠券延时消息，当达到用户预约的时间点后，向用户推送消息
        // 发送预约提醒抢购优惠券延时消息
        CouponTemplateRemindDelayEvent couponRemindDelayEvent = CouponTemplateRemindDelayEvent.builder()
                .couponTemplateId(couponTemplate.getId())
                .userId(UserContext.getUserId())
                .contact(UserContext.getUserId())
                .shopNumber(couponTemplate.getShopNumber())
                .type(requestParam.getType())
                .remindTime(requestParam.getRemindTime())
                .startTime(couponTemplate.getValidStartTime())
                .delayTime(DateUtil.offsetMinute(couponTemplate.getValidStartTime(), -requestParam.getRemindTime()).getTime())
                .build();
        couponTemplateRemindDelayProducer.sendMessage(couponRemindDelayEvent);

        // 删除用户预约提醒的缓存信息，通过更新数据库删除缓存策略保障数据库和缓存一致性
        stringRedisTemplate.delete(EngineRedisConstant.USER_COUPON_TEMPLATE_REMIND_INFORMATION + UserContext.getUserId());


    }

    @Override
    public List<CouponTemplateRemindQueryRespDTO> listCouponRemind(CouponTemplateRemindQueryReqDTO requestParam) {
        return List.of();
    }

    @Override
    public void cancelCouponRemind(CouponTemplateRemindCancelReqDTO requestParam) {
        // 验证优惠券是否存在，避免缓存穿透问题并获取优惠券开抢时间
        CouponTemplateQueryRespDTO couponTemplate = couponTemplateService
                .findCouponTemplate(new CouponTemplateQueryReqDTO(requestParam.getShopNumber(), requestParam.getCouponTemplateId()));
        if (couponTemplate.getValidStartTime().before(new Date())) {
            throw new ClientException("无法取消已开始领取的优惠券预约");
        }

        LambdaQueryWrapper<CouponTemplateRemindDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateRemindDO.class)
                .eq(CouponTemplateRemindDO::getUserId, UserContext.getUserId())
                .eq(CouponTemplateRemindDO::getCouponTemplateId, requestParam.getCouponTemplateId());
        CouponTemplateRemindDO couponTemplateRemindDO = couponTemplateRemindMapper.selectOne(queryWrapper);
        if (couponTemplateRemindDO == null) {
            throw new ClientException("优惠券模板预约信息不存在");
        }

        // 计算 BitMap 信息
        Long bitMap = CouponTemplateRemindUtil.calculateBitMap(requestParam.getRemindTime(), requestParam.getType());
        if ((bitMap & couponTemplateRemindDO.getInformation()) == 0L) {
            throw new ClientException("您没有预约该时间点的提醒");
        }

        // 异或运算，对应位已存在，异或结果为0，实现取消提醒
        bitMap ^= couponTemplateRemindDO.getInformation();
        queryWrapper.eq(CouponTemplateRemindDO::getInformation, couponTemplateRemindDO.getInformation());
        if (bitMap.equals(0L)) {
            // 如果新 BitMap 信息是 0，说明已经没有预约提醒了，可以直接删除
            if (couponTemplateRemindMapper.delete(queryWrapper) == 0) {
                throw new ClientException("取消提醒失败，请刷新页面后重试");
            }
        } else {
            // 虽然删除了这个预约提醒，但还有其它提醒，那就更新数据库
            couponTemplateRemindDO.setInformation(bitMap);
            if (couponTemplateRemindMapper.update(couponTemplateRemindDO, queryWrapper) == 0) {
                throw new ClientException("取消提醒失败，请刷新页面后重试");
            }
        }

        /**
         * 由于 RocketMQ 消费者在执行预约提醒时都需要查看该提醒是否已被取消，所以这里需要将用户取消提醒的记录写入布隆过滤器，减轻数据库压力
         * hash(couponTemplateId + userId + remindType + remindTime)
         */
        cancelRemindBloomFilter.add(String.valueOf(Objects.hash(requestParam.getCouponTemplateId(), UserContext.getUserId(), requestParam.getRemindTime(), requestParam.getType())));

        // 删除用户预约提醒的缓存信息，通过更新数据库删除缓存策略保障数据库和缓存一致性
        stringRedisTemplate.delete(EngineRedisConstant.USER_COUPON_TEMPLATE_REMIND_INFORMATION + UserContext.getUserId());

    }

    @Override
    public void cancelCouponRemindV2(CouponTemplateRemindCancelReqDTO requestParam) {
        // 验证优惠券是否存在，避免缓存穿透问题并获取优惠券开抢时间
        CouponTemplateQueryRespDTO couponTemplate = couponTemplateService
                .findCouponTemplate(new CouponTemplateQueryReqDTO(requestParam.getShopNumber(), requestParam.getCouponTemplateId()));
        if (couponTemplate.getValidStartTime().before(new Date())) {
            throw new ClientException("无法取消已开始领取的优惠券预约");
        }

        LambdaQueryWrapper<CouponTemplateRemindDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateRemindDO.class)
                .eq(CouponTemplateRemindDO::getUserId, UserContext.getUserId())
                .eq(CouponTemplateRemindDO::getCouponTemplateId, requestParam.getCouponTemplateId());
        CouponTemplateRemindDO couponTemplateRemindDO = couponTemplateRemindMapper.selectOne(queryWrapper);
        if (couponTemplateRemindDO == null) {
            throw new ClientException("优惠券模板预约信息不存在");
        }

        // 计算 BitMap 信息
        Long bitMap = CouponTemplateRemindUtil.calculateBitMap(requestParam.getRemindTime(), requestParam.getType());
        if ((bitMap & couponTemplateRemindDO.getInformation()) == 0L) {
            throw new ClientException("您没有预约该时间点的提醒");
        }

        // 异或运算，对应位已存在，异或结果为0，实现取消提醒
        bitMap ^= couponTemplateRemindDO.getInformation();
        queryWrapper.eq(CouponTemplateRemindDO::getInformation, couponTemplateRemindDO.getInformation());
        if (bitMap.equals(0L)) {
            // 如果新 BitMap 信息是 0，说明已经没有预约提醒了，可以直接删除
            if (couponTemplateRemindMapper.delete(queryWrapper) == 0) {
                throw new ClientException("取消提醒失败，请刷新页面后重试");
            }
        } else {
            // 虽然删除了这个预约提醒，但还有其它提醒，那就更新数据库
            couponTemplateRemindDO.setInformation(bitMap);
            if (couponTemplateRemindMapper.update(couponTemplateRemindDO, queryWrapper) == 0) {
                throw new ClientException("取消提醒失败，请刷新页面后重试");
            }
        }

        /**
         * 由于 RocketMQ 消费者在执行预约提醒时都需要查看该提醒是否已被取消，所以这里需要将用户取消提醒的记录写入布隆过滤器，减轻数据库压力
         * hash(couponTemplateId + userId + remindType + remindTime)
         */
        cancelRemindBloomFilter.add(String.valueOf(Objects.hash(requestParam.getCouponTemplateId(), UserContext.getUserId(), requestParam.getRemindTime(), requestParam.getType())));

        // todo: 删除用户预约提醒的缓存信息，这里通过 canal 监听实现，这里就不做缓存删除了
        // 其他保证缓存一致性的方案
    }

    @Override
    public boolean isCancelRemind(CouponTemplateRemindDTO requestParam) {
        if (!cancelRemindBloomFilter.contains(String.valueOf(Objects.hash(requestParam.getCouponTemplateId(), requestParam.getUserId(), requestParam.getRemindTime(), requestParam.getType())))) {
            // 布隆过滤器中不存在，说明没取消提醒，此时已经能挡下大部分请求
            return false;
        }

        // 对于少部分的“取消了预约”，可能是误判，此时需要去数据库中查找
        LambdaQueryWrapper<CouponTemplateRemindDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateRemindDO.class)
                .eq(CouponTemplateRemindDO::getUserId, requestParam.getUserId())
                .eq(CouponTemplateRemindDO::getCouponTemplateId, requestParam.getCouponTemplateId());
        CouponTemplateRemindDO couponTemplateRemindDO = couponTemplateRemindMapper.selectOne(queryWrapper);
        if (couponTemplateRemindDO == null) {
            // 数据库中没该条预约提醒，说明被取消
            return true;
        }

        // 即使存在数据，也要检查该类型的该时间点是否有提醒
        Long information = couponTemplateRemindDO.getInformation();
        Long bitMap = CouponTemplateRemindUtil.calculateBitMap(requestParam.getRemindTime(), requestParam.getType());

        // 按位与等于 0 说明用户取消了预约
        return (bitMap & information) == 0L;
    }
}
