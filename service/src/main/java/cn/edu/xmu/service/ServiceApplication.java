package cn.edu.xmu.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 服务模块启动类
 */
@SpringBootApplication(scanBasePackages = {"cn.edu.xmu.service", "cn.edu.xmu.javaee.core"})
@EnableFeignClients  // 启用OpenFeign
public class ServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }
}

