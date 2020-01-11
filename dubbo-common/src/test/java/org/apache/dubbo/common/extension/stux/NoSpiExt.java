package org.apache.dubbo.common.extension.stux;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;

/**
 * @Auther: wuxiuzhao
 * @Date: 2020/1/11 09:49
 * @Description:
 */
public interface NoSpiExt {
    String hello(URL url, String s);
}
