-- 定义一个最大值和位数
local SECOND_FIELD_BITS = 13

-- 将两个字段合成一个 int
local function combineFields(firstField, secondField)
    local firstFieldValue = firstField and 1 or 0
    return (firstFieldValue * 2 ^ SECOND_FIELD_BITS) + secondField
end

-- KEYS[1]: 优惠券模板的key -> coupon:cache:template:exist:{couponTemplateId}
-- KEYS[2]: 优惠券分发任务中已经领用该优惠券模板的用户的集合的key -> coupon:cache:distribution:task-execute-batch-user:{couponTaskId}
-- ARGV[1]: 用户 ID 和 Excel 所在行号 map类型 {userid: xxxx, rowNum:xxxx}


local key = KEYS[1] -- Redis Key
local userSetKey =  KEYS[2] -- 用户领券 Set 的 Key
local userIdAndRowNum = ARGV[1] -- 用户 ID 和 Excel 所在行号

-- 获取库存
local stock = tonumber(redis.call('HGET', key, 'stock'))

if stock == nil or stock <= 0 then
    return combineFields(false, redis.call('SCARD',  userSetKey))  -- redis.call('SCARD', userSetKey) 获取 userSetKey 这个集合的长度
end

-- 自减库存
redis.call('HINCRBY', key, 'stack', -1)

-- 添加用户到领券集合
redis.call('SADD', userSetKey, userIdAndRowNum)

-- 获取用户领券集合的长度
local userSetLength = redis.call('SCARD', userSetKey)

-- 返回结果
return combineFields(true, userSetLength)











