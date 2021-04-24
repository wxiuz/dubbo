package org.apache.dubbo.demo.provider.impl;

import org.apache.dubbo.demo.UserService;
import org.apache.dubbo.demo.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生服务实现类
 *
 * @author wuxiuzhao
 * @date 2021-04-22 21:23
 */
public class StudentServiceImpl implements UserService {

    private static final Map<Integer, User> STUDENT_MAP;

    static {
        STUDENT_MAP = new HashMap<>();
        STUDENT_MAP.put(1, new User(1, "小明", "学生"));
        STUDENT_MAP.put(2, new User(2, "小花", "学生"));
        STUDENT_MAP.put(3, new User(3, "小翠", "学生"));
    }

    @Override
    public List<User> queryAll() {
        return new ArrayList<>(STUDENT_MAP.values());
    }

    @Override
    public User findById(Integer id) {
        return STUDENT_MAP.get(id);
    }
}
