package com.gfd_sse.dummyoff2onredis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableMongoRepositories(basePackages = "com.gfd_sse.dummyoff2onredis.repository")
public class DummyOff2onRedisApplication {

  public static void main(String[] args) {
    SpringApplication.run(DummyOff2onRedisApplication.class, args);
  }

}
