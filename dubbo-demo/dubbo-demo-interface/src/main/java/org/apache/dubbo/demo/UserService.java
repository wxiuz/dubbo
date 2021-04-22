package org.apache.dubbo.demo;

import org.apache.dubbo.demo.model.User;

import java.util.List;

/**
 * @author wuxiuzhao
 * @date 2021-04-22 21:18
 */
public interface UserService {
    /**
     * 查询所有用户
     *
     * @return
     */
    List<User> queryAll();

    /**
     * 根据ID查询用户
     *
     * @param id
     * @return
     */
    User findById(Integer id);
}
