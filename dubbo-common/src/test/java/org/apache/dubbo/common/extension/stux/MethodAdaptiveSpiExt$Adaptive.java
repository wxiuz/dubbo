package org.apache.dubbo.common.extension.stux;

import org.apache.dubbo.common.extension.ExtensionLoader;

public class MethodAdaptiveSpiExt$Adaptive implements org.apache.dubbo.common.extension.stux.MethodAdaptiveSpiExt {
    public void hello(org.apache.dubbo.common.URL arg0, java.lang.String arg1) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        org.apache.dubbo.common.URL url = arg0;
        String extName = url.getParameter("adaptive-impl", "method-adaptive");
        if (extName == null)
            throw new IllegalStateException("Failed to get extension (org.apache.dubbo.common.extension.stux.MethodAdaptiveSpiExt) name from url (" + url.toString() + ") use keys([adaptive-impl])");
        org.apache.dubbo.common.extension.stux.MethodAdaptiveSpiExt extension = (org.apache.dubbo.common.extension.stux.MethodAdaptiveSpiExt) ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.extension.stux.MethodAdaptiveSpiExt.class).getExtension(extName);
        extension.hello(arg0, arg1);
    }

    public void export(org.apache.dubbo.common.URL arg0) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        org.apache.dubbo.common.URL url = arg0;
        String extName = (url.getProtocol() == null ? "method-adaptive" : url.getProtocol());
        if (extName == null)
            throw new IllegalStateException("Failed to get extension (org.apache.dubbo.common.extension.stux.MethodAdaptiveSpiExt) name from url (" + url.toString() + ") use keys([protocol])");
        org.apache.dubbo.common.extension.stux.MethodAdaptiveSpiExt extension = (org.apache.dubbo.common.extension.stux.MethodAdaptiveSpiExt) ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.extension.stux.MethodAdaptiveSpiExt.class).getExtension(extName);
        extension.export(arg0);
    }
}