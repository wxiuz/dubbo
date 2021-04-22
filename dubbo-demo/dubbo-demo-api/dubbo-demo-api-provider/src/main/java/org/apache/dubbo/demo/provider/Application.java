/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.dubbo.demo.provider;

import com.google.common.collect.Lists;
import org.apache.dubbo.config.*;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.demo.UserService;

/**
 * @author lenovo
 */
public class Application {
    public static void main(String[] args) throws Exception {
        // 应用配置，代表当前应用信息
        ApplicationConfig application = new ApplicationConfig("dubbo-demo-api-provider");
        application.setOwner("wuxiuzhao");
        application.setCompiler("jdk");

        // 协议配置
        ProtocolConfig dubboProtocol = new ProtocolConfig();
        dubboProtocol.setName("dubbo");
        dubboProtocol.setDispatcher("all");
        dubboProtocol.setHost("127.0.0.1");
        dubboProtocol.setPort(9100);
        dubboProtocol.setRegister(true);
        dubboProtocol.setThreadpool("fixed");
        dubboProtocol.setThreads(100);

        // 应用提供的服务
        ServiceConfig<UserService> studentService = new ServiceConfig<>();
        studentService.setId("studentService");
        studentService.setInterface(UserService.class);
        studentService.setRef(new StudentServiceImpl());
        studentService.setProxy("jdk");
        studentService.setProtocols(Lists.newArrayList(dubboProtocol));
        // 一个Service不同的实现通过group来区分
        studentService.setGroup("student");
        studentService.setVersion("V1.0.0");

        ServiceConfig<UserService> teacherService = new ServiceConfig<>();
        teacherService.setId("teacherService");
        teacherService.setInterface(UserService.class);
        teacherService.setRef(new TeacherServiceImpl());
        teacherService.setProxy("jdk");
        teacherService.setProtocols(Lists.newArrayList(dubboProtocol));
        teacherService.setGroup("teacher");
        teacherService.setVersion("V1.0.0");

        // 注册中心配置，代表当前服务需要将服务信息注册到的地址
        RegistryConfig registry = new RegistryConfig("zookeeper://127.0.0.1:2181?backup=127.0.0.1:2181,127.0.0.1:2181");

        ConfigCenterConfig configCenterConfig = new ConfigCenterConfig();

        // dubbo服务端，用于监听客户端的请求
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        // 配置应用信息
        bootstrap.application(application)
                // 配置注册中心
                .registry(registry)
                // 配置中心
                .configCenter(configCenterConfig)
                // 配置 服务提供者
                .services(Lists.newArrayList(studentService, teacherService))
                // 启动dubbo服务端
                .start()
                .await();
    }
}
