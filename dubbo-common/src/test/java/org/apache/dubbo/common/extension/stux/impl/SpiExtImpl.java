package org.apache.dubbo.common.extension.stux.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.stux.SpiExt;

/**
 * @Auther: wuxiuzhao
 * @Date: 2020/1/11 10:02
 * @Description:
 */
public class SpiExtImpl implements SpiExt {
    @Override
    public String hello(URL url, String s) {
        System.out.println("execute spi hello -----> hello " + s);
        return s;
    }
}
