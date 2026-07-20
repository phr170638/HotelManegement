package com.hotel.common.lock;

import org.redisson.api.RLock;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ImmediateFailLockTimeoutStrategy implements LockTimeoutStrategy {

    public static final String STRATEGY_NAME = "immediateFail";

    @Override
    public String strategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public boolean tryLock(RLock lock) throws InterruptedException {
        return lock.tryLock(0, 5, TimeUnit.SECONDS);
    }
}
