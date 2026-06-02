package com.jobradar.backend.global.cache;

import com.jobradar.backend.global.lock.RedisLockExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class DistributedCacheLoader {

    private static final String LOCK_PREFIX = "jobradar:lock:cache:";

    private final CacheManager cacheManager;
    private final RedisLockExecutor redisLockExecutor;

    public <T> T getOrLoad(String cacheName, String key, Supplier<T> loader) {
        Cache cache = getCache(cacheName);
        T cached = getCached(cache, key);
        if (cached != null) {
            return cached;
        }

        return redisLockExecutor.executeWithLock(lockKey(cacheName, key), () -> {
            T cachedAgain = getCached(cache, key);
            if (cachedAgain != null) {
                return cachedAgain;
            }

            T loaded = loader.get();
            cache.put(key, loaded);
            return loaded;
        });
    }

    private Cache getCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new IllegalArgumentException("Cache not found: " + cacheName);
        }
        return cache;
    }

    @SuppressWarnings("unchecked")
    private <T> T getCached(Cache cache, String key) {
        Cache.ValueWrapper wrapper = cache.get(key);
        return wrapper != null ? (T) wrapper.get() : null;
    }

    private String lockKey(String cacheName, String key) {
        return LOCK_PREFIX + cacheName + ":" + key;
    }
}
