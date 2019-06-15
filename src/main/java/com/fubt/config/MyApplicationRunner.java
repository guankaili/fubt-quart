package com.fubt.config;

import com.fubt.service.RobotConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * <p>@Description: </p>
 *
 * @date 2019/5/193:23 PM
 */
@Component
public class MyApplicationRunner implements ApplicationRunner {

    @Autowired
    private RobotConfigService robotConfigService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        robotConfigService.cleanRobotStatus();
    }
}
