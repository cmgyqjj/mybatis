/*
 *    Copyright 2009-2023 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.cache.decorators;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

/**
 * <p>
 * Simple blocking decorator
 * <p>
 * Simple and inefficient version of EhCache's BlockingCache decorator. It sets a lock over a cache key when the element
 * is not found in cache. This way, other threads will wait until this element is filled instead of hitting the
 * database.
 * <p>
 * By its nature, this implementation can cause deadlock when used incorrectly.
 *
 * @author Eduardo Macarron
 */
public class BlockingCache implements Cache {

    //    阻塞超时时间
    private long timeout;
//    被装饰的底层Cache对象
    private final Cache delegate;
//    每个key有对应的锁
    private final ConcurrentHashMap<Object, CountDownLatch> locks;

    public BlockingCache(Cache delegate) {
        this.delegate = delegate;
        this.locks = new ConcurrentHashMap<>();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }

    @Override
    public void putObject(Object key, Object value) {
        try {
            delegate.putObject(key, value);
        } finally {
            releaseLock(key);
        }
    }

    @Override
    public Object getObject(Object key) {
//        尝试获取锁
        acquireLock(key);
//        取到缓存对象
        Object value = delegate.getObject(key);
//        不为空的话就释放锁，否则就继续持有
        if (value != null) {
            releaseLock(key);
        }
        return value;
    }

    @Override
    public Object removeObject(Object key) {
        // despite its name, this method is called only to release locks
        releaseLock(key);
        return null;
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    private void acquireLock(Object key) {
//        写屏障
        CountDownLatch newLatch = new CountDownLatch(1);
        while (true) {
//            尝试把锁放进去，如果没有对应的对象就创建一个，之前用的是ReentrantLock
            CountDownLatch latch = locks.putIfAbsent(key, newLatch);
            if (latch == null) {
                break;
            }
            try {
//                设置阻塞超时时间然后阻塞，或者不设置直接阻塞
                if (timeout > 0) {
                    boolean acquired = latch.await(timeout, TimeUnit.MILLISECONDS);
                    if (!acquired) {
                        throw new CacheException(
                            "Couldn't get a lock in " + timeout + " for the key " + key + " at the cache " + delegate.getId());
                    }
                } else {
                    latch.await();
                }
            } catch (InterruptedException e) {
                throw new CacheException("Got interrupted while trying to acquire lock for key " + key, e);
            }
        }
    }

    private void releaseLock(Object key) {
//        先从map里面删除key，然后释放一个锁
        CountDownLatch latch = locks.remove(key);
        if (latch == null) {
            throw new IllegalStateException("Detected an attempt at releasing unacquired lock. This should never happen.");
        }
        latch.countDown();
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
