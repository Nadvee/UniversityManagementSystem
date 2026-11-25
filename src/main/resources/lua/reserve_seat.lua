local seats = redis.call('GET', KEYS[1])
if not seats then return -1 end
if tonumber(seats) <= 0 then return 0 end
redis.call('DECR', KEYS[1])
return 1
