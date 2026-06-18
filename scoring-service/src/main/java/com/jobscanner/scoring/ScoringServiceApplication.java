package com.jobscanner.scoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaRetryTopic;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafkaRetryTopic
@EnableScheduling
public class ScoringServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScoringServiceApplication.class, args);
    }
}
