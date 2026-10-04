package com.yansh.platform.payment;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.yansh.platform.payment", "com.yansh.platform.common"})
public class PaymentServiceApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}