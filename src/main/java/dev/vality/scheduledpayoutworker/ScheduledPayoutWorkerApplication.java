package dev.vality.scheduledpayoutworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@ServletComponentScan
@SpringBootApplication
public class ScheduledPayoutWorkerApplication extends SpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScheduledPayoutWorkerApplication.class, args);
    }

}
