package com.example.coupon.engine.toolkit;

import cn.hutool.core.date.DateUtil;
import com.example.coupon.engine.common.enums.CouponRemindTypeEnum;
import com.example.coupon.engine.dto.resp.CouponTemplateQueryRespDTO;
import com.example.coupon.engine.dto.resp.CouponTemplateRemindQueryRespDTO;
import com.example.coupon.framework.exception.ClientException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 优惠券预约提醒工具类
 * <p>
 *     采用位图存储用户预约信息
 *     假设提前 60分钟开启预约提醒，每隔 5分钟可以设置一次提醒，那么每种通知方式就需要 12 个提醒时间点。
 *     假设有 5 种通知方式，那么共需要 60 种组合
 *     采用位图存储用户预约信息，每种预约方式占用 12 个 bit 位，每个 bit 位代表 5分钟间隔，共计 60分钟
 *     5 种通知方式仅需要一个 Long 类型即可
 * </p>
 */
public class CouponTemplateRemindUtil {

    /**
     * 下一个预约类型的位移量，每个预约类型占用 12个 bit 位，每个 bit 位代表5分钟间隔，共计 60分钟
     */
    private static final int NEXT_TYPE_BITS = 12;

    /**
     * 预约时间间隔
     */
    private static final int TIME_INTERVAL = 5;

    /**
     * 提醒方式的数量，这里目前只有 2 种，采用 long 类型为后续留下扩展
     */
    private static final int TYPE_COUNT = CouponRemindTypeEnum.values().length;


    /**
     * 填充预约信息
     */
    public static void fillRemindInformation(CouponTemplateRemindQueryRespDTO resp, Long information) {
        List<Date> dateList = new ArrayList<>();
        List<String> remindType = new ArrayList<>();
        Date validStartTime = resp.getValidStartTime();
        for (int i = NEXT_TYPE_BITS - 1; i >= 0; i--) {
            // 按时间节点倒叙遍历，即离开抢时间最久，离现在最近
            for (int j = 0; j < TYPE_COUNT; j++) {
                // 对于每个时间节点，遍历所有类型
                if (((information >> (j * NEXT_TYPE_BITS + i)) & 1) == 1) {
                    // 该时间节点的该提醒类型用户有预约
                    Date date = DateUtil.offsetMinute(validStartTime, -((i + 1) * TIME_INTERVAL));
                    dateList.add(date);
                    remindType.add(CouponRemindTypeEnum.getDescribeByType(j));
                }
            }
        }
        resp.setRemindTime(dateList);
        resp.setRemindType(remindType);
    }

    /**
     * 根据预约时间和预约类型计算位图
     * @param remindTime 预约时间
     * @param type 预约类型
     * @return 位图
     */
    public static Long calculateBitMap(Integer remindTime, Integer type) {
        if (remindTime > TIME_INTERVAL * NEXT_TYPE_BITS) {
            throw new ClientException("预约提醒的时间不能早于活动开始前" + TIME_INTERVAL * NEXT_TYPE_BITS + "分钟");
        }
        // 计算占用哪12位
        int typeIndex = type * NEXT_TYPE_BITS;
        // 计算哪几个 5 分钟间隔的位
        int timeIndex = Math.max(0, remindTime / TIME_INTERVAL - 1);
        return 1L << (typeIndex + timeIndex);
    }


}
