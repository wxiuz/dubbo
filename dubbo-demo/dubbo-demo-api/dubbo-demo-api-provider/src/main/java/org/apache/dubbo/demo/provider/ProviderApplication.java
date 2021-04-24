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
import org.apache.dubbo.demo.HelloService;
import org.apache.dubbo.demo.UserService;
import org.apache.dubbo.demo.provider.impl.HelloServiceImpl;
import org.apache.dubbo.demo.provider.impl.StudentServiceImpl;
import org.apache.dubbo.demo.provider.impl.TeacherServiceImpl;

/**
 * @author lenovo
 */
public class ProviderApplication {
    public static void main(String[] args) throws Exception {
        int protocolPort = 9200;
        // 全局默认应用配置（当Service没有直接指定时使用）
        ApplicationConfig application = new ApplicationConfig("dubbo-demo-api-provider");
        application.setOwner("wuxiuzhao");
        application.setCompiler("jdk");

        // 全局默认注册中心配置（当Service没有直接指定时使用）
        RegistryConfig registry = new RegistryConfig("zookeeper://127.0.0.1:2181?backup=127.0.0.1:2181,127.0.0.1:2181");

        // 全局默认协议配置（当Service没有配置时使用）
        ProtocolConfig dubboProtocol = new ProtocolConfig();
        dubboProtocol.setName("dubbo");
        dubboProtocol.setDispatcher("all");
        dubboProtocol.setHost("127.0.0.1");
        dubboProtocol.setPort(protocolPort);
        dubboProtocol.setRegister(true);
        dubboProtocol.setThreadpool("fixed");
        dubboProtocol.setThreads(50);
        dubboProtocol.setIothreads(10);

        ProviderConfig provider = new ProviderConfig();
        provider.setProtocol(dubboProtocol);
        provider.setProxy("jdk");
        provider.setDispatcher("all");
        provider.setRegistry(registry);

        /**
         * 所有服务都可以单独指定protocol, registry等
         */
        // 应用提供的服务
        ServiceConfig<UserService> studentService = new ServiceConfig<>();
        studentService.setId("studentService");
        studentService.setInterface(UserService.class);
        studentService.setRef(new StudentServiceImpl());
        studentService.setProxy("jdk");
        // 一个Service不同的实现通过group来区分
        studentService.setGroup("student");
        studentService.setVersion("V1.0.0");

        ServiceConfig<UserService> teacherService = new ServiceConfig<>();
        teacherService.setId("teacherService");
        teacherService.setInterface(UserService.class);
        teacherService.setRef(new TeacherServiceImpl());
        teacherService.setProxy("jdk");
        teacherService.setGroup("teacher");
        teacherService.setVersion("V1.0.0");

        ServiceConfig<HelloService> helloService = new ServiceConfig<>();
        helloService.setId("helloService");
        helloService.setInterface(HelloService.class);
        helloService.setRef(new HelloServiceImpl());
        helloService.setProxy("jdk");
        helloService.setGroup("demo");
        helloService.setVersion("V1.0.0");

        // dubbo服务端，用于监听客户端的请求
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        // 配置应用信息
        bootstrap.application(application)
                // 服务提供者
                .provider(provider)
                .protocol(dubboProtocol)
                .registry(registry)
                // 配置 服务提供者
                .services(Lists.newArrayList(studentService, teacherService, helloService))
                // 启动dubbo服务端
                .start()
                .await();
    }
}
