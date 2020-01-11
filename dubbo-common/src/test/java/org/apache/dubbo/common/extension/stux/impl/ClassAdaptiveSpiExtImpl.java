package org.apache.dubbo.common.extension.stux.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.stux.ClassAdaptiveSpiExt;

/**
 * @Auther: wuxiuzhao
 * @Date: 2020/1/11 11:47
 * @Description:
 */
@Adaptive
public class ClassAdaptiveSpiExtImpl implements ClassAdaptiveSpiExt {

    @Override
    public void hello(URL url, String s) {
        System.out.println("adaptive -----> hello " + s);
    }
}
