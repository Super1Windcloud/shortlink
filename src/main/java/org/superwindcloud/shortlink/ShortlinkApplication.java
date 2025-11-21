package org.superwindcloud.shortlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ShortlinkApplication {

  public static void main(String[] args) {
    SpringApplication.run(ShortlinkApplication.class, args);
  }
}
