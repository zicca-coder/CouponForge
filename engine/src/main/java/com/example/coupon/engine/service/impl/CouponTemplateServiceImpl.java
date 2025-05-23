package com.example.coupon.engine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.coupon.engine.common.constant.EngineRedisConstant;
import com.example.coupon.engine.common.enums.CouponTemplateStatusEnum;
import com.example.coupon.engine.dao.entity.CouponTemplateDO;
import com.example.coupon.engine.dao.mapper.CouponTemplateMapper;
import com.example.coupon.engine.dto.req.CouponTemplateQueryReqDTO;
import com.example.coupon.engine.dto.resp.CouponTemplateQueryRespDTO;
import com.example.coupon.engine.service.CouponTemplateService;
import com.example.coupon.framework.exception.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 优惠券模板业务逻辑实现层
 */
@Slf4j(topic = "CouponTemplateServiceImpl")
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplateDO> implements CouponTemplateService {


    private final CouponTemplateMapper couponTemplateMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final RBloomFilter<String> couponTemplateBloomFilter;

    /**
     * 查询优惠券模板
     * Note: 此处为用户调用查询接口，可能存在缓存击穿、穿透问题
     *
     * @param requestParam 请求参数
     * @return 优惠券模板详情
     */
    @Override
    public CouponTemplateQueryRespDTO findCouponTemplate(CouponTemplateQueryReqDTO requestParam) {
        // 查询缓存中否有优惠券模板信息
        String couponTemplateCacheKey = EngineRedisConstant.COUPON_TEMPLATE_KEY + requestParam.getCouponTemplateId();
        // 返回 Key 为  coupon:template:{couponTemplateId} 的Hash表的所有字段和值，以Map<Object, Object>的形式返回
        Map<Object, Object> couponTemplateCacheMap = stringRedisTemplate.opsForHash().entries(couponTemplateCacheKey);

        // 如果存在直接返回，不存在则通过布隆过滤器、缓存空值以及双重判定锁的形式读取数据库中的记录
        if (MapUtil.isEmpty(couponTemplateCacheMap)) {
            // 判断布隆过滤器是否存在指定模板 ID，不存在直接返回错误
            if (!couponTemplateBloomFilter.contains(requestParam.getCouponTemplateId())) {
                log.error("【布隆过滤器】中无该ID:{}，判断该优惠券模板不存在...", requestParam.getCouponTemplateId());
                throw new ClientException("优惠券模板不存在，请检查操作是否正确...");
            }

            // 查询 Redis COUPON_TEMPLATE_NOT-EXIST缓存中是否有
            String couponTemplateNotExistCacheKey = EngineRedisConstant.COUPON_TEMPLATE_NOT_EXIST + requestParam.getCouponTemplateId();
            Boolean hasKeyFlag = stringRedisTemplate.hasKey(couponTemplateNotExistCacheKey);
            if (hasKeyFlag) {
                log.error("第一重判定 -> 【NOT-EXIST缓存】中存在该优惠券 ID:{}", requestParam.getCouponTemplateId());
                throw new ClientException("优惠券模板不存在，请检查操作是否正确...");
            }

            // 查询数据库
            // 获取优惠券模板分布式锁
            RLock lock = redissonClient.getLock(EngineRedisConstant.COUPON_TEMPLATE_LOCK_KEY + requestParam.getCouponTemplateId());
            lock.lock();
            try {
                // 多个线程同时抢占分布式锁，最终只能有一个线程获取锁，并执行缓存重建（若数据库存在，则重建缓存若数据库中不存在在假如NOT-EXIST缓存）
                // 当第一个线程执行完毕释放锁之后，后续线程获取到锁，此时就没必要再查询数据库，因此直接判断缓存中是否存在，跳过数据库查询
                hasKeyFlag = stringRedisTemplate.hasKey(couponTemplateNotExistCacheKey);
                if (hasKeyFlag) {
                    log.error("第二重判定 -> 【NOT-EXIST缓存】中存在该优惠券 ID:{}", requestParam.getCouponTemplateId());
                    throw new ClientException("优惠券模板不存在，请检查操作是否正确...");
                }
                couponTemplateCacheMap = stringRedisTemplate.opsForHash().entries(couponTemplateCacheKey);

                if (MapUtil.isEmpty(couponTemplateCacheMap)) {
                    CouponTemplateDO couponTemplateDO = lambdaQuery().eq(CouponTemplateDO::getId, requestParam.getCouponTemplateId())
                            .eq(CouponTemplateDO::getShopNumber, requestParam.getShopNumber())
                            .eq(CouponTemplateDO::getStatus, CouponTemplateStatusEnum.ACTIVE.getStatus())
                            .one();
                    // 如果数据库中没有该记录，添加该 id 到 NOT-EXIST 缓存中
                    if (couponTemplateDO == null) {
                        log.error("数据库中不存在该优惠券模板，将该ID:{} 缓存到【NOT-EXIST缓存】中", requestParam.getCouponTemplateId());
                        stringRedisTemplate.opsForValue().set(couponTemplateNotExistCacheKey, "", 30, TimeUnit.MINUTES);
                        throw new ClientException("优惠券模板不存在，请检查操作是否正确...");
                    }
                    CouponTemplateQueryRespDTO actualRespDTO = BeanUtil.toBean(couponTemplateDO, CouponTemplateQueryRespDTO.class);
                    Map<String, Object> cacheTargetMap = BeanUtil.beanToMap(actualRespDTO, false, true);
                    Map<String, String> actualCacheTargetMap = cacheTargetMap.entrySet().stream().
                            collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue() != null ? entry.getValue().toString() : ""));

                    // 通过 LUA 脚本执行设置 Hash 数据以及设置过期时间
                    String luaScript = "redis.call('HMSET', KEYS[1], unpack(ARGV, 1, #ARGV - 1)) " +
                            "redis.call('EXPIREAT', KEYS[1], ARGV[#ARGV])";

                    List<String> keys = Collections.singletonList(couponTemplateCacheKey);
                    List<String> args = new ArrayList<>(actualCacheTargetMap.size() * 2 + 1);
                    actualCacheTargetMap.forEach((key, value) -> {
                        args.add(key);
                        args.add(value);
                    });

                    // 优惠券活动过期时间转换为秒级别的 Unix 时间戳
                    args.add(String.valueOf(couponTemplateDO.getValidEndTime().getTime() / 1000));

                    // 执行 LUA 脚本
                    stringRedisTemplate.execute(
                            new DefaultRedisScript<>(luaScript, Long.class),
                            keys,
                            args.toArray()
                    );
                    couponTemplateCacheMap = cacheTargetMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                }
            } finally {
                lock.unlock();
            }

        }
        return BeanUtil.mapToBean(couponTemplateCacheMap, CouponTemplateQueryRespDTO.class, false, CopyOptions.create());
    }

    @Override
    public List<CouponTemplateDO> listCouponTemplateByIds(List<Long> couponTemplateIds, List<Long> shopNumbers) {
        return List.of();
    }
}
