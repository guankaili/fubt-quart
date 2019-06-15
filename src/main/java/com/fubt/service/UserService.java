package com.fubt.service;

import com.fubt.dao.UserDao;
import com.fubt.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>@Description: </p>
 *
 * @date 2019/5/1410:27 PM
 */
@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    public List<User> allRobots() {
        return userDao.all();
    }

    public void addUser(User user) {
        userDao.insert(user);
    }

    public void update(User user) {
        userDao.updateById(user);
    }

    public User getUserById(int id) {
        return userDao.single(id);
    }

    public void deleteById(Integer id) {
        userDao.deleteById(id);
    }
}
