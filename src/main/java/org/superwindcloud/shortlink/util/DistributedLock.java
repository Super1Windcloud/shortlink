package org.superwindcloud.shortlink.util;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class DistributedLock {

  private final StringRedisTemplate redisTemplate;

  private static final long DEFAULT_EXPIRE_TIME = 10; // 10秒

  public DistributedLock(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * 尝试获取分布式锁
   *
   * @param lockKey 锁的键
   * @param lockValue 锁的值（建议使用UUID）
   * @param expireTime 锁的过期时间（秒）
   * @return 是否获取成功
   */
  public boolean tryLock(String lockKey, String lockValue, long expireTime) {
    Boolean result =
        redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expireTime, TimeUnit.SECONDS);
    return result != null && result;
  }

  /**
   * 释放分布式锁
   *
   * @param lockKey 锁的键
   * @param lockValue 锁的值
   * @return 是否释放成功
   */
  public boolean releaseLock(String lockKey, String lockValue) {
    String script =
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    Long result =
        redisTemplate.execute(
            (RedisCallback<Long>)
                connection ->
                    connection.eval(
                        script.getBytes(StandardCharsets.UTF_8),
                        ReturnType.INTEGER,
                        1,
                        lockKey.getBytes(StandardCharsets.UTF_8),
                        lockValue.getBytes(StandardCharsets.UTF_8)));
    return result != null && result == 1;
  }

  /**
   * 尝试获取分布式锁（使用默认过期时间）
   *
   * @param lockKey 锁的键
   * @param lockValue 锁的值
   * @return 是否获取成功
   */
  public boolean tryLock(String lockKey, String lockValue) {
    return tryLock(lockKey, lockValue, DEFAULT_EXPIRE_TIME);
  }
}
