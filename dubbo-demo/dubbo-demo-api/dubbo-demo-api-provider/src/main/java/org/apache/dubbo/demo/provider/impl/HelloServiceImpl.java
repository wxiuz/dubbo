package org.apache.dubbo.demo.provider.impl;

import org.apache.dubbo.demo.HelloService;

import java.util.concurrent.CompletableFuture;

/**
 * @author wuxiuzhao
 * @date 2021-04-22 22:17
 */
public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(String msg) {
        return "hello " + msg;
    }

    @Override
    public CompletableFuture<String> asyncHello(String msg) {
        return CompletableFuture.completedFuture("hello " + msg);
    }
}
