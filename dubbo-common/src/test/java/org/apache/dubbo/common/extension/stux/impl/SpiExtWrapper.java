package org.apache.dubbo.common.extension.stux.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.stux.SpiExt;

/**
 * @Auther: wuxiuzhao
 * @Date: 2020/1/11 11:33
 * @Description:
 */
public class SpiExtWrapper implements SpiExt {

    private SpiExt ext;

    public SpiExtWrapper(SpiExt ext) {
        this.ext = ext;
    }

    @Override
    public String hello(URL url, String s) {
        System.out.println("spi ext wrapper : aop ---- begin");
        final String hello = ext.hello(url, s);
        System.out.println("spi ext wrapper : aop ---- end");
        return hello;
    }
}
