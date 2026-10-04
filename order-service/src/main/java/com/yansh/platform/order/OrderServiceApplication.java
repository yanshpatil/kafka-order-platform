package com.yansh.platform.order;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.yansh.platform.order", "com.yansh.platform.common"})
@EnableScheduling
public class OrderServiceApplication {

    public static void main(String[] args) {
        // Run in UTC: avoids Postgres rejecting legacy zone names such as "Asia/Calcutta"
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}