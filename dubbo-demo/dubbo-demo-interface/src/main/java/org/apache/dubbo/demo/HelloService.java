package org.apache.dubbo.demo;

import java.util.concurrent.CompletableFuture;

/**
 * @author wuxiuzhao
 * @date 2021-04-22 22:13
 */
public interface HelloService {

    /**
     * 同步hello
     *
     * @param msg
     * @return
     */
    String hello(String msg);

    /**
     * 异步hello
     *
     * @param msg
     * @return
     */
    CompletableFuture<String> asyncHello(String msg);
}
