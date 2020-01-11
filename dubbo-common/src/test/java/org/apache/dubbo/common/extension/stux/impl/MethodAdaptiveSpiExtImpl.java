package org.apache.dubbo.common.extension.stux.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.stux.MethodAdaptiveSpiExt;

/**
 * @Auther: wuxiuzhao
 * @Date: 2020/1/11 11:54
 * @Description:
 */
public class MethodAdaptiveSpiExtImpl implements MethodAdaptiveSpiExt {
    @Override
    public void hello(URL url, String s) {
        System.out.println("method adaptive actual method execute " + s);
    }

    @Override
    public void export(URL url) {
        System.out.println("method adaptive actual method execute ");
    }
}
