package com.school.fees;

import io.mongock.runner.springboot.EnableMongock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstraps the Fees Service microservice.
 */
@SpringBootApplication
@EnableMongock
public class FeesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeesServiceApplication.class, args);
    }
}
