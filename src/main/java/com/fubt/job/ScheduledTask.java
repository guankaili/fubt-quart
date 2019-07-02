package com.fubt.job;

import com.fubt.service.RobotConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * <p>@Description: </p>
 *
 * @author guankaili
 * @date 2019/6/2511:06 PM
 */
@Component
public class ScheduledTask {

    @Autowired
    private RobotConfigService robotConfigService;
//    @Scheduled(cron="0 0/5 * * * ?")
    public void Trading(){
        robotConfigService.doTrading(1);
    }
}
