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
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.demo.DemoService;

/**
 * @author lenovo
 */
public class Application {
    public static void main(String[] args) throws Exception {
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
        ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());
        service.setProxy("jdk");
        service.setProtocols(Lists.newArrayList(dubboProtocol));

        // 应用配置，代表当前应用信息
        ApplicationConfig application = new ApplicationConfig("dubbo-demo-api-provider");
        application.setOwner("wuxiuzhao");

        // 注册中心配置，代表当前服务需要将服务信息注册到的地址
        RegistryConfig registry = new RegistryConfig("zookeeper://127.0.0.1:2181?backup=127.0.0.1:2181,127.0.0.1:2181");
        registry.setCheck(true);

        // dubbo服务端，用于监听客户端的请求
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();

        bootstrap
                .application(application) // 配置应用信息
                .registry(registry) // 配置注册中心
                .service(service)  // 注册服务
                .start()  // 启动dubbo服务端
                .await();
    }
}
