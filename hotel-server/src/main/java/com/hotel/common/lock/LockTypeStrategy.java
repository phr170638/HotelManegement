package com.hotel.common.lock;

import org.redisson.api.RLock;

public interface LockTypeStrategy {

    boolean supports(LockType lockType);

    RLock getLock(String lockKey);
}
