package com.fubt.job;

import com.fubt.dao.RobotConfigDao;
import com.fubt.entity.RobotConfig;
import com.fubt.service.RobotConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>@Description: </p>
 *
 * @author guankaili
 * @date 2019/6/2511:06 PM
 */
@Component
public class ScheduledTask {
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private RobotConfigDao robotConfigDao;
    @Autowired
    private RobotConfigService robotConfigService;
    @Scheduled(cron="0 0/5 * * * ?")
    public void Trading(){
        List<RobotConfig> robotConfigs = robotConfigDao.all();
        if(!CollectionUtils.isEmpty(robotConfigs)){
            for(RobotConfig robot : robotConfigs){
                if (robot.getStatus() == 0) {
                    logger.info("{} 市场的机器人 {} 已经停止！", robot.getSymbol(), robot.getName());
                    return ;
                }
                robotConfigService.doTrading(robot);
            }
        }

    }
}
