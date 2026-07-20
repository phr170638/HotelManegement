package com.hotel.common.lock;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReentrantLockTypeStrategy implements LockTypeStrategy {

    private final ObjectProvider<RedissonClient> redissonClientProvider;

    @Override
    public boolean supports(LockType lockType) {
        return LockType.REENTRANT == lockType;
    }

    @Override
    public RLock getLock(String lockKey) {
        return redissonClientProvider.getObject().getLock(lockKey);
    }
}
