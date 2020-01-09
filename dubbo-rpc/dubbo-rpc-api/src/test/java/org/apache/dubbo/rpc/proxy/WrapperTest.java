package org.apache.dubbo.rpc.proxy;

import org.apache.dubbo.common.bytecode.Wrapper;
import org.junit.jupiter.api.Test;

/**
 * @Auther: wuxiuzhao
 * @Date: 2020/1/8 16:50
 * @Description:
 */
public class WrapperTest {
    @Test
    public void test() {
        final Wrapper wrapper = Wrapper.getWrapper(DemoService.class);
    }
}
