package com.hotel.common.lock;

import org.redisson.api.RLock;

public interface LockTimeoutStrategy {

    String strategyName();

    boolean tryLock(RLock lock) throws InterruptedException;
}
