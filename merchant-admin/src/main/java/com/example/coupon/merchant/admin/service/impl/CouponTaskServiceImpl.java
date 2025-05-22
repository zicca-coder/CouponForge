package com.example.coupon.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.coupon.framework.exception.ClientException;
import com.example.coupon.merchant.admin.common.context.UserContext;
import com.example.coupon.merchant.admin.common.enums.CouponTaskSendTypeEnum;
import com.example.coupon.merchant.admin.common.enums.CouponTaskStatusEnum;
import com.example.coupon.merchant.admin.common.enums.CouponTemplateStatusEnum;
import com.example.coupon.merchant.admin.dao.entity.CouponTaskDO;
import com.example.coupon.merchant.admin.dao.mapper.CouponTaskMapper;
import com.example.coupon.merchant.admin.dto.req.CouponTaskCreateReqDTO;
import com.example.coupon.merchant.admin.dto.req.CouponTaskPageQueryReqDTO;
import com.example.coupon.merchant.admin.dto.resp.CouponTaskQueryRespDTO;
import com.example.coupon.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;
import com.example.coupon.merchant.admin.mq.event.CouponTaskExecuteEvent;
import com.example.coupon.merchant.admin.mq.producer.CouponTaskActualExecuteProducer;
import com.example.coupon.merchant.admin.service.CouponTaskService;
import com.example.coupon.merchant.admin.service.CouponTemplateService;
import com.example.coupon.merchant.admin.service.handler.excel.RowCountListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class CouponTaskServiceImpl extends ServiceImpl<CouponTaskMapper, CouponTaskDO> implements CouponTaskService {

    private final CouponTaskMapper couponTaskMapper;
    private final CouponTemplateService couponTemplateService;
    private final RedissonClient redissonClient;
    private final CouponTaskActualExecuteProducer couponTaskActualExecuteProducer;

    /**
     *
     */
    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(), // 核心线程数
            Runtime.getRuntime().availableProcessors() << 1, // 最大线程数
            60, // 线程空闲时间
            TimeUnit.SECONDS,
            new SynchronousQueue<>(), // 任务阻塞队列
            new ThreadPoolExecutor.DiscardPolicy() // 拒绝策略
    );

    /**
     * 创建优惠券推送任务
     *
     * @param requestParam 请求参数
     */
    // todo: @LogRecord
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createCouponTask(CouponTaskCreateReqDTO requestParam) {
        // todo: 基于责任链验证创建推送任务的参数是否正确
        // 验证非空参数
        // 验证参数是否正确，比如文件地址是否为我们期望的格式等
        // 验证参数依赖关系，比如选择定时发送，等
        // ....
        CouponTemplateQueryRespDTO couponTemplate = couponTemplateService.findCouponTemplateById(requestParam.getCouponTemplateId());
        if (couponTemplate == null) {
            throw new ClientException("优惠券模板不存在，请检查操作是否正确...");
        }
        if (ObjectUtil.notEqual(couponTemplate.getStatus(), CouponTemplateStatusEnum.ACTIVE.getStatus())) {
            throw new ClientException("优惠券模板已停用，请勿重复操作...");
        }

        // 构建优惠券推送任务实体
        CouponTaskDO couponTaskDO = BeanUtil.copyProperties(requestParam, CouponTaskDO.class);
        couponTaskDO.setBatchId(IdUtil.getSnowflakeNextId());
        couponTaskDO.setOperatorId(Long.parseLong(UserContext.getUserId()));
        couponTaskDO.setShopNumber(UserContext.getShopNumber());
        couponTaskDO.setStatus(
                Objects.equals(requestParam.getSendType(), CouponTaskSendTypeEnum.IMMEDIATE.getType())
                        ? CouponTaskStatusEnum.IN_PROGRESS.getStatus()
                        : CouponTaskStatusEnum.PENDING.getStatus()
        );
        // 保存优惠券推送任务记录到数据库
        // Note: 构建推送任务参数中无推送目标总人数，需要读取 Excel 行数来获取，但读取操作耗时（100万条大概4s），因此先插入数据库，后续通过线程池执行更新操作为优惠券发送数量赋值
        couponTaskMapper.insert(couponTaskDO);

        JSONObject delayJsonObject = JSONObject.of("fileAddress", requestParam.getFileAddress(), "couponTaskId", couponTaskDO.getId());
        // 创建线程池执行更新优惠券推送任务发送行数
        // Note: @Transactional 的作用范围是主线程执行的方法体
        // executorService.execute(() -> {}) 启动了一个新的线程，不受当前事务的控制，因此无法回滚
        executorService.execute(() -> {
            refreshCouponTaskSendNum(delayJsonObject);
        });

        // 假设刚把消息提交到线程池，突然应用宕机了，这里通过延迟队列进行兜底
        RBlockingDeque<Object> blockingDeque = redissonClient.getBlockingDeque("COUPON_TASK_SEND_NUM_DELAY_QUEUE");
        RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
        // 这里延迟时间设置 20 秒，实际生产中可以动态配置，比如根据任务类型，或者任务量动态配置延迟时间
        delayedQueue.offer(delayJsonObject, 20, TimeUnit.SECONDS);


        // 如果是立即推送型任务，直接调用消息队列进行发送流程
        // 如果是定时推送型任务，则采用 xxl-job 定时扫描触发
        if (ObjectUtil.equals(requestParam.getSendType(), CouponTaskSendTypeEnum.IMMEDIATE.getType())) {
            // 执行优惠券推送业务，正式向用户发放优惠券
            CouponTaskExecuteEvent couponTaskExecuteEvent = CouponTaskExecuteEvent.builder().couponTaskId(couponTaskDO.getId()).build();
            couponTaskActualExecuteProducer.sendMessage(couponTaskExecuteEvent);
        }
    }

    @Override
    public IPage<CouponTaskQueryRespDTO> pageQueryCouponTask(CouponTaskPageQueryReqDTO requestParam) {
        return null;
    }

    @Override
    public CouponTaskQueryRespDTO findCouponTaskById(String taskId) {
        return null;
    }

    private void refreshCouponTaskSendNum(JSONObject delayJsonObject) {
        // 通过 Excel 监听获取 Excel 中所有行数
        RowCountListener listener = new RowCountListener();
        EasyExcel.read(delayJsonObject.getString("fileAddress"), listener).sheet().doRead();
        int totalRows = listener.getRowCount();

        // 刷新优惠券推送记录中发送行数
        CouponTaskDO updateCouponTaskDO = CouponTaskDO.builder().id(delayJsonObject.getLong("couponTaskId")).sendNum(totalRows).build();
        couponTaskMapper.updateById(updateCouponTaskDO);
    }


    /**
     * 当 CouponServiceImpl 启动时，会自动执行该方法，启动一个线程，一直监听延迟队列，如果延迟队列有元素，则执行更新优惠券推送任务发送行数的逻辑
     */
    @PostConstruct
    public void init() {
        new RefreshCouponTaskDelayQueueRunner(this, couponTaskMapper, redissonClient).run();
    }

    /**
     * 优惠券延迟刷新发送条数兜底消费者 | 兜底策略
     */
    @RequiredArgsConstructor
    static class RefreshCouponTaskDelayQueueRunner {
        private final CouponTaskServiceImpl couponTaskService;
        private final CouponTaskMapper couponTaskMapper;
        private final RedissonClient redissonClient;

        /**
         * 开启线程，一直监听延迟队列，如果延迟队列有元素，则执行更新优惠券推送任务发送行数的逻辑
         */
        public void run() {
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("delay_coupon-task_send-num_consumer");
                thread.setDaemon(Boolean.TRUE);
                return thread;
            }).execute(() -> {
                RBlockingDeque<JSONObject> blockingDeque = redissonClient.getBlockingDeque("COUPON_TASK_SEND_NUM_DELAY_QUEUE");
                for (; ; ) {
                    try {
                        // 获取延迟队列已达时间元素
                        JSONObject delayJsonObject = blockingDeque.take();
                        if (delayJsonObject != null) {
                            // 获取优惠券推送记录，查看发送条数是否已经有值，有的话代表上面线程池已经处理完，无需再处理
                            CouponTaskDO couponTaskDO = couponTaskMapper.selectById(delayJsonObject.getLong("couponTaskId"));
                            if (couponTaskDO.getSendNum() == null) {
                                couponTaskService.refreshCouponTaskSendNum(delayJsonObject);
                            }
                        }
                    } catch (Throwable ignored) {

                    }
                }
            });
        }

    }


}
