package org.apache.dubbo.demo;

/**
 * @Auther: wuxiuzhao
 * @Date: 2020/1/11 15:38
 * @Description:
 */
public class DemoServiceStub implements DemoService {

    private DemoService service;

    public DemoServiceStub(DemoService service) {
        this.service = service;
    }

    @Override
    public String sayHello(String name) {
        System.err.println("===================== Demo Stub ");
        return service.sayHello(name);
    }
}
