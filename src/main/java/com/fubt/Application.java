package com.fubt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * <p>@Description: 启动类，其他项目引入该模块时，需要屏蔽该启动类</p>
 *
 * @date 2018/1/22下午4:17
 */
@SpringBootApplication(scanBasePackages = { "com.fubt" })
//@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
