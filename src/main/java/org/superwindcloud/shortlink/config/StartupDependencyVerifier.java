package org.superwindcloud.shortlink.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class StartupDependencyVerifier implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(StartupDependencyVerifier.class);

  private final DataSource dataSource;
  private final StringRedisTemplate redisTemplate;

  public StartupDependencyVerifier(DataSource dataSource, StringRedisTemplate redisTemplate) {
    this.dataSource = dataSource;
    this.redisTemplate = redisTemplate;
  }

  @Override
  public void run(String... args) throws Exception {
    verifyPostgres();
    verifyRedis();
  }

  private void verifyPostgres() throws Exception {
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT 1");
        ResultSet rs = ps.executeQuery()) {
      if (!rs.next() || rs.getInt(1) != 1) {
        throw new IllegalStateException("PostgreSQL connectivity check failed: unexpected result");
      }
      log.info("PostgreSQL connectivity verified");
    }
  }

  private void verifyRedis() {
    try {
      String pong = redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
      if (!"PONG".equalsIgnoreCase(pong)) {
        throw new IllegalStateException(
            "Redis connectivity check failed: unexpected response " + pong);
      }
      log.info("Redis connectivity verified");
    } catch (DataAccessException e) {
      throw new IllegalStateException("Redis connectivity check failed", e);
    }
  }
}
