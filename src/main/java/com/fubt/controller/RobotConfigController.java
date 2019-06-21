package com.fubt.controller;

import com.fubt.entity.RobotConfig;
import com.fubt.service.RobotConfigService;
import com.fubt.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.List;

/**
 * <p>@Description: </p>
 *
 * @date 2019/5/1410:30 PM
 */
@Controller
@RequestMapping("/robot")
public class RobotConfigController {

    @Autowired
    private RobotConfigService robotConfigService;

    @RequestMapping(value = "/start/{id}", method = RequestMethod.GET)
    @ResponseBody
    public R start(@PathVariable(value = "id") int id) {
        RobotConfig robotConfig = robotConfigService.getRobotConfigById(id);
        if (robotConfig == null) {
            return R.error("机器人不存在");
        }

        robotConfigService.start(id);

        return R.ok("[" + robotConfig.getSymbol() + "] 市场的机器人 [" + robotConfig.getName() + "] 已经成功启动");
    }

    @RequestMapping(value = "/stop/{id}", method = RequestMethod.GET)
    @ResponseBody
    public R stop(@PathVariable(value = "id") int id) {
        RobotConfig robotConfig = robotConfigService.getRobotConfigById(id);
        if (robotConfig == null) {
            return R.error("机器人不存在");
        }

        robotConfig.setStatus(0);
        robotConfigService.update(robotConfig);
        robotConfigService.cleanAtomic();

        return R.ok("[" + robotConfig.getSymbol() + "] 市场的机器人 [" + robotConfig.getName() + "] 已经停止");
    }

    @RequestMapping(value = "/robots", method = RequestMethod.GET)
    @ResponseBody
    public ModelAndView robots() {
        ModelAndView modelAndView = new ModelAndView("robot.html");
        List<RobotConfig> robots = robotConfigService.allRobots();
        modelAndView.addObject("robots", robots);
        return modelAndView;
    }

    @RequestMapping(value = "/add", method = RequestMethod.GET)
    public ModelAndView add() {
        ModelAndView modelAndView = new ModelAndView("robot_add.html");
        return modelAndView;
    }

    @RequestMapping(value = "/update/{id}", method = RequestMethod.GET)
    public ModelAndView update(@PathVariable(value = "id") Integer id) {
        ModelAndView modelAndView = new ModelAndView("robot_add.html");
        RobotConfig robotConfig = robotConfigService.getRobotConfigById(id);
        modelAndView.addObject("robot", robotConfig);
        return modelAndView;
    }

    @RequestMapping(value = "/submit", method = RequestMethod.GET)
    public ModelAndView submit(RobotConfig robotConfig) {

        // happy lucky
//        robotConfig.setSymbol("NECFBT");

        if (robotConfig.getId() == null || robotConfig.getId() < 0) {
            robotConfig.setCreateTime(new Date());
            robotConfig.setStatus(0);
            robotConfigService.add(robotConfig);
        } else {
            robotConfig.setCreateTime(new Date());
            robotConfigService.update(robotConfig);
        }

        ModelAndView modelAndView = new ModelAndView("redirect:/robot/robots");
        return modelAndView;
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    public ModelAndView delete(@PathVariable(value = "id") Integer id) {
        robotConfigService.deleteById(id);
        ModelAndView modelAndView = new ModelAndView("redirect:/robot/robots");
        return modelAndView;
    }
}
