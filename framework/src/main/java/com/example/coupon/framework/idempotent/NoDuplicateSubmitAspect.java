package com.example.coupon.framework.idempotent;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.example.coupon.framework.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 防止用户重复提交表单信息切面控制器
 */
@Aspect
@RequiredArgsConstructor
public class NoDuplicateSubmitAspect {

    private final RedissonClient redissonClient;
    private final RedisTemplate redisTemplate;


    @Around("@annotation(com.example.coupon.framework.idempotent.NoDuplicateSubmit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        NoDuplicateSubmit noDuplicateSubmit = getNoDuplicateSubmitAnnotation(joinPoint);
        // 获取幂等控制标识
        String idempotentKey = String.format("no-duplicate-submit:idempotent:path:%s:currentUserId:%s:md5:%s", getServletPath(), getCurrentUserId(), calcArgsMD5(joinPoint));
        if (redisTemplate.hasKey(idempotentKey)) {
            throw new ClientException(noDuplicateSubmit.message());
        }
        // 获取分布式锁标识
        String lockKey = String.format("no-duplicate-submit:lock:path:%s:currentUserId:%s:md5:%s", getServletPath(), getCurrentUserId(), calcArgsMD5(joinPoint));
        RLock lock = redissonClient.getLock(lockKey);
        // 尝试获取锁，如果获取失败，则证明重复提交，抛出异常
        if (!lock.tryLock()) {
            throw new ClientException(noDuplicateSubmit.message());
        }
        Object result;
        try {
            // 执行加注解方法的原逻辑
            result = joinPoint.proceed();
            redisTemplate.opsForValue().set(idempotentKey, "1", 5, TimeUnit.MINUTES); // 设置幂等时间
        } finally {
            lock.unlock();
        }
        return result;
    }


    /**
     * 获取当前方法上的 {@link NoDuplicateSubmit} 注解
     */
    public static NoDuplicateSubmit getNoDuplicateSubmitAnnotation(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = joinPoint.getTarget().getClass().getDeclaredMethod(methodSignature.getName(), methodSignature.getMethod().getParameterTypes());
        return method.getAnnotation(NoDuplicateSubmit.class);
    }

    /**
     * 获取当前线程上下文 ServletPath
     */
    private String getServletPath() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes.getRequest().getServletPath();
    }

    /**
     * 获取当前用户id
     */
    private String getCurrentUserId() {
        // 用户属于非核心功能，先模拟代替，后续重构代码
        return "1810518709471555585";
    }


    private String calcArgsMD5(ProceedingJoinPoint joinPoint) {
        return DigestUtil.md5Hex(JSON.toJSONBytes(joinPoint.getArgs()));
    }

}
