-- KEYS[1] 用户 ID 前缀
-- ARGV[1] 用户 ID 集合，JSON 格式
-- ARGV[2] 优惠券 ID 集合，JSON 格式

local userIds = cjson.decode(ARGV[1])  -- 解析 JSON
local couponIds = cjson.decode(ARGV[2]) -- 解析 JSON
local userIdPrefix = KEYS[1]
local currentTime = tonumber(ARGV[3])

for i, userId in ipairs(userIds) do
    local key = userIdPrefix .. userId -- 拼接用户 ID 前缀和用户 ID
    local couponId = couponIds[i] -- 获取优惠券 ID
    if couponId then
        -- ZADD  key score member 用户领取优惠券的时间作为 score
        redis.call('ZADD', key, currentTime, couponId) -- 添加优惠券 ID 到 ZSet 中
    end
end