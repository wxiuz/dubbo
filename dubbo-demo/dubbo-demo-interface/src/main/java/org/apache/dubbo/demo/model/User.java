package org.apache.dubbo.demo.model;

import java.io.Serializable;

/**
 * @author wuxiuzhao
 * @date 2021-04-22 21:20
 */
public class User implements Serializable {
    private Integer id;

    private String name;

    private String type;

    public User() {
    }

    public User(Integer id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
