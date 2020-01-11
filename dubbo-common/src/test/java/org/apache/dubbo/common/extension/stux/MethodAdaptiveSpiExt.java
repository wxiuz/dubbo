package org.apache.dubbo.common.extension.stux;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;

/**
 * @Auther: wuxiuzhao
 * @Date: 2020/1/11 11:52
 * @Description:
 */
@SPI("method-adaptive")
public interface MethodAdaptiveSpiExt {

    /**
     * @param url
     * @param s
     */
    @Adaptive("adaptive-impl")
    void hello(URL url, String s);

    /**
     * 根据URL中的protocol参数来获取实现
     */
    @Adaptive("protocol")
    void export(URL url);
}
