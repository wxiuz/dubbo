package org.apache.dubbo.common.extension;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.stux.ClassAdaptiveSpiExt;
import org.apache.dubbo.common.extension.stux.MethodAdaptiveSpiExt;
import org.apache.dubbo.common.extension.stux.NoSpiExt;
import org.apache.dubbo.common.extension.stux.SpiExt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @Auther: wuxiuzhao
 * @Date: 2020/1/11 09:47
 * @Description:
 */
public class ExtensionLoaderTests {

    @Test
    public void testNoSpi() {
        try {
            final ExtensionLoader<NoSpiExt> extensionLoader = ExtensionLoader.getExtensionLoader(NoSpiExt.class);
            final NoSpiExt nospi = extensionLoader.getExtension("nospi");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("Spi"));
        }
    }

    @Test
    public void testSpiExt() {
        final ExtensionLoader<SpiExt> extensionLoader = ExtensionLoader.getExtensionLoader(SpiExt.class);
        final SpiExt spiExt = extensionLoader.getExtension("spi-ext");
        System.out.println(spiExt.hello(null, "spi"));
    }

    @Test
    public void testDefaultAndWrapperSpiExt() {
        final ExtensionLoader<SpiExt> extensionLoader = ExtensionLoader.getExtensionLoader(SpiExt.class);
        final SpiExt spiExt = extensionLoader.getExtension("true");
        System.out.println(spiExt);
        spiExt.hello(null, "spi");
    }

    @Test
    public void testClassAdaptiveSpiExt() {
        final ExtensionLoader<ClassAdaptiveSpiExt> extensionLoader = ExtensionLoader.getExtensionLoader(ClassAdaptiveSpiExt.class);
        final ClassAdaptiveSpiExt adaptiveExtension = extensionLoader.getAdaptiveExtension();
        adaptiveExtension.hello(null, "adaptive spi ext");
    }

    @Test
    public void testMethodAdaptiveSpiExt() {
        final ExtensionLoader<MethodAdaptiveSpiExt> extensionLoader = ExtensionLoader.getExtensionLoader(MethodAdaptiveSpiExt.class);
        final MethodAdaptiveSpiExt adaptiveExtension = extensionLoader.getAdaptiveExtension();
        URL url = URL.valueOf("http://www.baidu.com");
        url = url.addParameter("adaptive-impl", "method-adaptive");
        System.out.println(url.toFullString());
        adaptiveExtension.hello(url, " method adaptive spi ext");
    }
}
