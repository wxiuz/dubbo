package org.apache.dubbo.common.extension.stux;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;

/**
 * @Auther: wuxiuzhao
 * @Date: 2020/1/11 11:46
 * @Description:
 */
@SPI
public interface ClassAdaptiveSpiExt {

    void hello(URL url, String s);
}
