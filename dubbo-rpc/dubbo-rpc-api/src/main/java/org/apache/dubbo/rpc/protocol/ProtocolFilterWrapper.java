/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.rpc.*;

import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.REFERENCE_FILTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SERVICE_FILTER_KEY;

/**
 * Dubbo的AOP机制，通过Dubbo的自动AOP功能进行包装原始的协议实现，在服务导出时构建Filter调用链
 */
public class ProtocolFilterWrapper implements Protocol {

    private final Protocol protocol;

    /**
     * 用于Dubbo AOP
     *
     * @param protocol
     */
    public ProtocolFilterWrapper(Protocol protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null");
        }
        this.protocol = protocol;
    }


    /**
     * 构建一个Filter链条
     * <p>
     * <p>
     * <p>
     * Filter1------->Filter2------->Filter3------->......------->FilterN【调用原始Invoker】
     * ^
     * |
     * |
     * <p>
     * Invoker1------>Invoker2------>Invoker3------>.......------>InvokerN
     * <p>
     * 最终返回Invoker1，调用链路为
     * Invoker1-->Filter1-->Invoker2-->Filter2-->Invoker3-->Filter3-->......-->InvokerN-->FilterN--->原始Invoker【包含真实服务对象的Invoker】
     *
     * @param invoker
     * @param key
     * @param group
     * @param <T>
     * @return
     */
    private static <T> Invoker<T> buildInvokerChain(final Invoker<T> invoker, String key, String group) {
        Invoker<T> last = invoker;
        List<Filter> filters = ExtensionLoader.getExtensionLoader(Filter.class).getActivateExtension(invoker.getUrl(), key, group);

        if (!filters.isEmpty()) {
            for (int i = filters.size() - 1; i >= 0; i--) {
                final Filter filter = filters.get(i);
                last = new FilterWrapperInvoker<>(invoker, last, filter);
            }
        }
        return last;
    }

    @Override
    public int getDefaultPort() {
        return protocol.getDefaultPort();
    }

    /**
     * 服务端暴露服务
     *
     * @param invoker Service invoker
     * @param <T>
     * @return
     * @throws RpcException
     */
    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        /**
         * 服务注册直接过，不需要包装Filter
         */
        if (UrlUtils.isRegistry(invoker.getUrl())) {
            return protocol.export(invoker);
        }
        /**
         * 如果是真实服务暴露，则需要构建调用链，最后传给protocol的则是经过Filter包装过的Invoker对象，即
         * Invoker1-->Filter1-->Invoker2-->Filter2-->Invoker3-->Filter3-->......-->InvokerN-->FilterN--->原始Invoker【包含真实服务对象的Invoker】
         *
         * {@link org.apache.dubbo.rpc.filter.EchoFilter}
         * {@link org.apache.dubbo.rpc.filter.ClassLoaderFilter}
         * {@link org.apache.dubbo.rpc.filter.GenericFilter}
         * {@link org.apache.dubbo.rpc.filter.ContextFilter}
         * {@link org.apache.dubbo.rpc.filter.LogFilter}
         * {@link org.apache.dubbo.rpc.filter.TraceFilter}
         * {@link org.apache.dubbo.rpc.filter.TimeoutFilter}
         * {@link org.apache.dubbo.rpc.filter.MonitorFilter}
         * {@link org.apache.dubbo.rpc.filter.ExceptionFilter}
         */
        return protocol.export(buildInvokerChain(invoker, SERVICE_FILTER_KEY, CommonConstants.PROVIDER));
    }

    /**
     * 客户端应用服务
     *
     * @param type Service class
     * @param url  URL address for the remote service
     * @param <T>
     * @return
     * @throws RpcException
     */
    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        if (UrlUtils.isRegistry(url)) {
            return protocol.refer(type, url);
        }
        return buildInvokerChain(protocol.refer(type, url), REFERENCE_FILTER_KEY, CommonConstants.CONSUMER);
    }

    @Override
    public void destroy() {
        protocol.destroy();
    }

    @Override
    public List<ProtocolServer> getServers() {
        return protocol.getServers();
    }

}
