package com.gfd_sse.dummyoff2onredis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DummyOff2onRedisApplication {

  public static void main(String[] args) {
    SpringApplication.run(DummyOff2onRedisApplication.class, args);
  }

}
