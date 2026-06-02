package com.jobradar.backend.global.lock;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class RedisLockExecutor {

    private static final long DEFAULT_WAIT_SECONDS = 5;

    private final RedissonClient redissonClient;

    public <T> T executeWithLock(String key, Supplier<T> task) {
        RLock lock = redissonClient.getLock(key);
        boolean locked = false;

        try {
            locked = lock.tryLock(DEFAULT_WAIT_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                throw new LockAcquisitionException("Redis lock acquisition timed out: " + key);
            }

            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException("Redis lock acquisition interrupted: " + key, e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
