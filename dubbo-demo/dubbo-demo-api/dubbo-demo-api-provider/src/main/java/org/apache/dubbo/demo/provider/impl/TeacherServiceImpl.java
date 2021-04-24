package org.apache.dubbo.demo.provider.impl;

import org.apache.dubbo.demo.UserService;
import org.apache.dubbo.demo.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 老师服务实现类
 *
 * @author wuxiuzhao
 * @date 2021-04-22 21:29
 */
public class TeacherServiceImpl implements UserService {
    
    private static final Map<Integer, User> TEACHER_MAP;

    static {
        TEACHER_MAP = new HashMap<>();
        TEACHER_MAP.put(7, new User(7, "王老师", "老师"));
        TEACHER_MAP.put(8, new User(8, "张老师", "老师"));
        TEACHER_MAP.put(9, new User(9, "李老师", "老师"));
    }

    @Override
    public List<User> queryAll() {
        return new ArrayList<>(TEACHER_MAP.values());
    }

    @Override
    public User findById(Integer id) {
        return TEACHER_MAP.get(id);
    }
}
