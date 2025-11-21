package org.superwindcloud.shortlink.util;

import java.nio.charset.StandardCharsets;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

  private final StringRedisTemplate redisTemplate;

  public RateLimiter(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * 限流检查
   *
   * @param key 限流的键（如IP地址、用户ID等）
   * @param limit 限制次数
   * @param duration 限制时间窗口（秒）
   * @return 是否允许通过
   */
  public boolean isAllowed(String key, int limit, int duration) {
    String script =
        "local key = KEYS[1] "
            + "local limit = tonumber(ARGV[1]) "
            + "local window = tonumber(ARGV[2]) "
            + "local current = redis.call('GET', key) "
            + "if current == false then "
            + "  redis.call('SET', key, 1) "
            + "  redis.call('EXPIRE', key, window) "
            + "  return 1 "
            + "end "
            + "current = tonumber(current) "
            + "if current < limit then "
            + "  redis.call('INCR', key) "
            + "  return 1 "
            + "else "
            + "  return 0 "
            + "end";

    Long result =
        redisTemplate.execute(
            (RedisCallback<Long>)
                connection ->
                    connection.eval(
                        script.getBytes(StandardCharsets.UTF_8),
                        ReturnType.INTEGER,
                        1,
                        key.getBytes(StandardCharsets.UTF_8),
                        String.valueOf(limit).getBytes(StandardCharsets.UTF_8),
                        String.valueOf(duration).getBytes(StandardCharsets.UTF_8)));

    return result != null && result == 1;
  }
}
