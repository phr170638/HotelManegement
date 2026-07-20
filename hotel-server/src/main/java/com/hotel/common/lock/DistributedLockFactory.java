package com.hotel.common.lock;

import com.hotel.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Callable;

@Component
@RequiredArgsConstructor
public class DistributedLockFactory {

    private final List<LockTypeStrategy> lockTypeStrategies;
    private final List<LockTimeoutStrategy> lockTimeoutStrategies;

    public <T> T executeWithLock(LockType lockType, String lockKey, String timeoutStrategyName, Callable<T> callable) {
        RLock lock = resolveLockTypeStrategy(lockType).getLock(lockKey);
        boolean locked = false;
        try {
            locked = resolveTimeoutStrategy(timeoutStrategyName).tryLock(lock);
            if (!locked) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
            return callable.call();
        } catch (BusinessException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取分布式锁被中断");
        } catch (Exception exception) {
            throw new BusinessException("业务处理失败，请稍后重试");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private LockTypeStrategy resolveLockTypeStrategy(LockType lockType) {
        return lockTypeStrategies.stream()
                .filter(strategy -> strategy.supports(lockType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到匹配的锁类型策略"));
    }

    private LockTimeoutStrategy resolveTimeoutStrategy(String timeoutStrategyName) {
        return lockTimeoutStrategies.stream()
                .filter(strategy -> strategy.strategyName().equals(timeoutStrategyName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到匹配的锁超时策略"));
    }
}
