-- KEYS[1]: auction:option:{id}:stock (해당 회차의 재고 키)
-- ARGV[1]: 차감할 수량 (보통 1)

local stock = redis.call('GET', KEYS[1])

-- 1. 재고가 없거나 키가 존재하지 않는 경우
if not stock or tonumber(stock) <= 0 then
    return -1
end

-- 2. 재고 차감 시도
local current_stock = tonumber(stock)
local decrement_amount = tonumber(ARGV[1])

if current_stock < decrement_amount then
    return -1 -- 재고 부족
end

-- 3. 원자적 차감 수행
local remaining = redis.call('DECRBY', KEYS[1], decrement_amount)
return remaining -- 남은 재고 반환 (0 이상이면 성공)