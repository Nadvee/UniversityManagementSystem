if redis.call('EXISTS', KEYS[1]) == 1 then return redis.call('INCR', KEYS[1]) end
return -1
