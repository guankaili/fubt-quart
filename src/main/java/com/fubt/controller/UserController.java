package com.fubt.controller;

import com.fubt.entity.User;
import com.fubt.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.List;

/**
 * <p>@Description: </p>
 *
 * @date 2019/5/1410:29 PM
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public ModelAndView users() {
        ModelAndView modelAndView = new ModelAndView("user.html");
        List<User> robots = userService.allRobots();
        modelAndView.addObject("users", robots);
        return modelAndView;
    }

    @RequestMapping(value = "/add", method = RequestMethod.GET)
    public ModelAndView add() {
        ModelAndView modelAndView = new ModelAndView("user_add.html");
        return modelAndView;
    }

    @RequestMapping(value = "/update/{id}", method = RequestMethod.GET)
    public ModelAndView update(@PathVariable(value = "id") Integer id) {
        ModelAndView modelAndView = new ModelAndView("user_add.html");
        User user = userService.getUserById(id);
        modelAndView.addObject("user", user);
        return modelAndView;
    }

    @RequestMapping(value = "/submit", method = RequestMethod.GET)
    public ModelAndView submit(User user) {
        if (user.getId() == null || user.getId() < 0) {
            user.setCreateTime(new Date());
            userService.addUser(user);
        } else {
            user.setCreateTime(new Date());
            userService.update(user);
        }

        ModelAndView modelAndView = new ModelAndView("redirect:/user/users");
        return modelAndView;
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    public ModelAndView delete(@PathVariable(value = "id") Integer id) {
        userService.deleteById(id);
        ModelAndView modelAndView = new ModelAndView("redirect:/user/users");
        return modelAndView;
    }

}
