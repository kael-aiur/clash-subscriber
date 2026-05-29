package site.kael.clash.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "site.kael.clash")
public class ClashSubscriberApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClashSubscriberApplication.class, args);
    }
}
