package cn.edu.xmu.aftersale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 售后模块启动类
 */
@SpringBootApplication(scanBasePackages = {"cn.edu.xmu.aftersale", "cn.edu.xmu.javaee.core"})
@EnableFeignClients
public class AftersaleApplication {
    public static void main(String[] args) {
        SpringApplication.run(AftersaleApplication.class, args);
    }
}

