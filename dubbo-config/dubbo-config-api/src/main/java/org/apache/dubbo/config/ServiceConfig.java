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
package org.apache.dubbo.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.bytecode.Wrapper;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.*;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.event.ServiceConfigExportedEvent;
import org.apache.dubbo.config.event.ServiceConfigUnexportedEvent;
import org.apache.dubbo.config.invoker.DelegateProviderMetaDataInvoker;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.event.Event;
import org.apache.dubbo.event.EventDispatcher;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.ConfiguratorFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.REGISTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.RegistryConstants.DYNAMIC_KEY;
import static org.apache.dubbo.common.utils.NetUtils.*;
import static org.apache.dubbo.config.Constants.*;
import static org.apache.dubbo.remoting.Constants.BIND_IP_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;
import static org.apache.dubbo.rpc.Constants.*;
import static org.apache.dubbo.rpc.cluster.Constants.EXPORT_KEY;

/**
 * 服务提供者配置
 *
 * @param <T>
 */
public class ServiceConfig<T> extends ServiceConfigBase<T> {

    public static final Logger logger = LoggerFactory.getLogger(ServiceConfig.class);

    /**
     * A random port cache, the different protocols who has no port specified have different random port
     */
    private static final Map<String, Integer> RANDOM_PORT_MAP = new HashMap<String, Integer>();

    /**
     * 延迟服务暴露调度器
     */
    private static final ScheduledExecutorService DELAY_EXPORT_EXECUTOR = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("DubboServiceDelayExporter", true));

    /**
     * dubbo所有支持的服务导出协议
     */
    private static final Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    /**
     * 代理工厂类：用来创建代理类
     */
    private static final ProxyFactory PROXY_FACTORY = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    /**
     * Whether the provider has been exported
     */
    private transient volatile boolean exported;

    /**
     * The flag whether a service has unexported ,if the method unexported is invoked, the value is true
     */
    private transient volatile boolean unexported;

    private DubboBootstrap bootstrap;

    /**
     * The exported services
     */
    private final List<Exporter<?>> exporters = new ArrayList<Exporter<?>>();

    public ServiceConfig() {
    }

    public ServiceConfig(Service service) {
        super(service);
    }

    @Parameter(excluded = true)
    public boolean isExported() {
        return exported;
    }

    @Parameter(excluded = true)
    public boolean isUnexported() {
        return unexported;
    }

    public void unexport() {
        if (!exported) {
            return;
        }
        if (unexported) {
            return;
        }
        if (!exporters.isEmpty()) {
            for (Exporter<?> exporter : exporters) {
                try {
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn("Unexpected error occured when unexport " + exporter, t);
                }
            }
            exporters.clear();
        }
        unexported = true;

        // dispatch a ServiceConfigUnExportedEvent since 2.7.4
        dispatch(new ServiceConfigUnexportedEvent(this));
    }

    /**
     * 导出服务
     */
    public synchronized void export() {
        if (!shouldExport()) {
            return;
        }

        if (bootstrap == null) {
            bootstrap = DubboBootstrap.getInstance();
            bootstrap.init();
        }

        // 为Service配置ProviderConfig, ProtocolConfig, RegistryConfig
        checkAndUpdateSubConfigs();

        serviceMetadata.setVersion(version);
        serviceMetadata.setGroup(group);
        serviceMetadata.setDefaultGroup(group);
        serviceMetadata.setServiceType(getInterfaceClass());
        serviceMetadata.setServiceInterfaceName(getInterface());
        serviceMetadata.setTarget(getRef());

        if (shouldDelay()) {
            // 如果延迟导出，则延迟指定时间后再导出服务
            DELAY_EXPORT_EXECUTOR.schedule(this::doExport, getDelay(), TimeUnit.MILLISECONDS);
        } else {
            // 服务导出
            doExport();
        }
    }

    private void checkAndUpdateSubConfigs() {
        // 补充完成一些配置信息
        completeCompoundConfigs();
        // 为ServiceConfig设置ProviderConfig
        checkDefault();
        // 为ServiceConfig设置ProtocolConfig
        checkProtocol();
        // 如果Dubbo服务的协议不是injvm，则需要检查注册中心配置
        if (!isOnlyInJvm()) {
            // 为ServiceConfig设置RegistryConfig
            checkRegistry();
        }
        this.refresh();

        if (StringUtils.isEmpty(interfaceName)) {
            throw new IllegalStateException("<dubbo:service interface=\"\" /> interface not allow null!");
        }

        // 如果服务实现类为GenericService类型【通用Service类型】
        if (ref instanceof GenericService) {
            interfaceClass = GenericService.class;
            if (StringUtils.isEmpty(generic)) {
                generic = Boolean.TRUE.toString();
            }
        } else {
            try {
                interfaceClass = Class.forName(interfaceName, true, Thread.currentThread()
                        .getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            checkInterfaceAndMethods(interfaceClass, getMethods());
            checkRef();
            generic = Boolean.FALSE.toString();
        }
        if (local != null) {
            if ("true".equals(local)) {
                local = interfaceName + "Local";
            }
            Class<?> localClass;
            try {
                localClass = ClassUtils.forNameWithThreadContextClassLoader(local);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            if (!interfaceClass.isAssignableFrom(localClass)) {
                throw new IllegalStateException("The local implementation class " + localClass.getName() + " not implement interface " + interfaceName);
            }
        }
        if (stub != null) {
            if ("true".equals(stub)) {
                stub = interfaceName + "Stub";
            }
            Class<?> stubClass;
            try {
                stubClass = ClassUtils.forNameWithThreadContextClassLoader(stub);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            if (!interfaceClass.isAssignableFrom(stubClass)) {
                throw new IllegalStateException("The stub implementation class " + stubClass.getName() + " not implement interface " + interfaceName);
            }
        }
        checkStubAndLocal(interfaceClass);
        ConfigValidationUtils.checkMock(interfaceClass, this);
        ConfigValidationUtils.validateServiceConfig(this);
        appendParameters();
    }


    protected synchronized void doExport() {
        if (unexported) {
            throw new IllegalStateException("The service " + interfaceClass.getName() + " has already unexported!");
        }
        if (exported) {
            return;
        }
        exported = true;

        if (StringUtils.isEmpty(path)) {
            path = interfaceName;
        }

        // 导出服务
        doExportUrls();

        // dispatch a ServiceConfigExportedEvent since 2.7.4
        dispatch(new ServiceConfigExportedEvent(this));
    }

    /**
     * 导出服务
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doExportUrls() {
        ServiceRepository repository = ApplicationModel.getServiceRepository();
        ServiceDescriptor serviceDescriptor = repository.registerService(getInterfaceClass());
        repository.registerProvider(
                getUniqueServiceName(),
                ref,
                serviceDescriptor,
                this,
                serviceMetadata
        );

        // 获取注册中心地址，返回List说明支持多注册中心，多个注册中心之间用|或者;隔开
        // 地址格式为：registry://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?accepts=100&application=demo-provider&application.version=1.0.0&check=false&compiler=jdk&dubbo=2.0.2&owner=demo&pid=8540&qos.port=22222&register=true&registry=zookeeper&timestamp=1578572225516&version=1.1.0
        List<URL> registryURLs = ConfigValidationUtils.loadRegistries(this, true);

        /**
         * 一个服务可以通过多种协议暴露并注册到多个注册中心上
         * 每一种协议就是需要启动一个Server服务监听指定端口
         * 来接收请求
         */
        for (ProtocolConfig protocolConfig : protocols) {
            String pathKey = URL.buildKey(getContextPath(protocolConfig)
                    .map(p -> p + "/" + path)
                    .orElse(path), group, version);
            // In case user specified path, register service one more time to map it to path.
            repository.registerService(pathKey, interfaceClass);
            serviceMetadata.setServiceKey(pathKey);
            // 一个协议可以暴露到多个注册中心
            doExportUrlsFor1Protocol(protocolConfig, registryURLs);
        }
    }

    /**
     * 以某种协议暴露服务到多个注册中心
     *
     * @param protocolConfig 服务暴露的协议
     * @param registryURLs   服务注册到的注册中心，可能是多个注册中心
     */
    private void doExportUrlsFor1Protocol(ProtocolConfig protocolConfig, List<URL> registryURLs) {
        // 协议名称
        String name = protocolConfig.getName();

        // 如果没有配置，默认是dubbo协议
        if (StringUtils.isEmpty(name)) {
            name = DUBBO;
        }

        Map<String, String> map = new HashMap<String, String>();
        // 服务暴露，则说明当前是服务提供方
        map.put(SIDE_KEY, PROVIDER_SIDE);

        // 将相关配置的get方法返回值以参数的形式放到map中
        ServiceConfig.appendRuntimeParameters(map);
        AbstractConfig.appendParameters(map, metrics);
        AbstractConfig.appendParameters(map, application);
        AbstractConfig.appendParameters(map, module);
        // remove 'default.' prefix for configs from ProviderConfig
        // appendParameters(map, provider, Constants.DEFAULT_KEY);
        AbstractConfig.appendParameters(map, provider);
        AbstractConfig.appendParameters(map, protocolConfig);
        // 将ServiceConfig中配置信息放到Map中
        AbstractConfig.appendParameters(map, this);

        // 如果有方法配置，则需要解析方法配置
        if (CollectionUtils.isNotEmpty(getMethods())) {
            for (MethodConfig method : getMethods()) {
                AbstractConfig.appendParameters(map, method, method.getName());
                String retryKey = method.getName() + ".retry";
                if (map.containsKey(retryKey)) {
                    String retryValue = map.remove(retryKey);
                    if ("false".equals(retryValue)) {
                        map.put(method.getName() + ".retries", "0");
                    }
                }
                List<ArgumentConfig> arguments = method.getArguments();
                if (CollectionUtils.isNotEmpty(arguments)) {
                    for (ArgumentConfig argument : arguments) {
                        // convert argument type
                        if (argument.getType() != null && argument.getType().length() > 0) {
                            Method[] methods = interfaceClass.getMethods();
                            // visit all methods
                            if (methods != null && methods.length > 0) {
                                for (int i = 0; i < methods.length; i++) {
                                    String methodName = methods[i].getName();
                                    // target the method, and get its signature
                                    if (methodName.equals(method.getName())) {
                                        Class<?>[] argtypes = methods[i].getParameterTypes();
                                        // one callback in the method
                                        if (argument.getIndex() != -1) {
                                            if (argtypes[argument.getIndex()].getName().equals(argument.getType())) {
                                                AbstractConfig.appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                                            } else {
                                                throw new IllegalArgumentException("Argument config error : the index attribute and type attribute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                            }
                                        } else {
                                            // multiple callbacks in the method
                                            for (int j = 0; j < argtypes.length; j++) {
                                                Class<?> argclazz = argtypes[j];
                                                if (argclazz.getName().equals(argument.getType())) {
                                                    AbstractConfig.appendParameters(map, argument, method.getName() + "." + j);
                                                    if (argument.getIndex() != -1 && argument.getIndex() != j) {
                                                        throw new IllegalArgumentException("Argument config error : the index attribute and type attribute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (argument.getIndex() != -1) {
                            AbstractConfig.appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                        } else {
                            throw new IllegalArgumentException("Argument config must set index or type attribute.eg: <dubbo:argument index='0' .../> or <dubbo:argument type=xxx .../>");
                        }

                    }
                }
            } // end of methods for
        }

        if (ProtocolUtils.isGeneric(generic)) {
            map.put(GENERIC_KEY, generic);
            map.put(METHODS_KEY, ANY_VALUE);
        } else {
            String revision = Version.getVersion(interfaceClass, version);
            if (revision != null && revision.length() > 0) {
                map.put(REVISION_KEY, revision);
            }

            String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
            if (methods.length == 0) {
                logger.warn("No method found in service interface " + interfaceClass.getName());
                map.put(METHODS_KEY, ANY_VALUE);
            } else {
                // 当前服务暴露的方法，包括接口中的所有方法，多个方法通过逗号,分隔
                map.put(METHODS_KEY, StringUtils.join(new HashSet<String>(Arrays.asList(methods)), ","));
            }
        }
        if (!ConfigUtils.isEmpty(token)) {
            if (ConfigUtils.isDefault(token)) {
                map.put(TOKEN_KEY, UUID.randomUUID().toString());
            } else {
                map.put(TOKEN_KEY, token);
            }
        }
        serviceMetadata.getAttachments().putAll(map);

        // 获取服务提供者的ip
        String host = findConfigedHosts(protocolConfig, registryURLs, map);
        // 获取服务提供者的服务端口
        Integer port = findConfigedPorts(protocolConfig, name, map);

        /**
         * 根据暴露的协议创建服务暴露的URL，同一个服务协议不同，则该URL不同，同一个服务需要按照协议的维度来进行URL构建
         */
        URL url = new URL(name, host, port, getContextPath(protocolConfig).map(p -> p + "/" + path).orElse(path), map);

        // You can customize Configurator to append extra parameters
        if (ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                .hasExtension(url.getProtocol())) {
            url = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class)
                    .getExtension(url.getProtocol()).getConfigurator(url).configure(url);
        }

        String scope = url.getParameter(SCOPE_KEY);
        // don't export when none is configured
        if (!SCOPE_NONE.equalsIgnoreCase(scope)) {
            if (!SCOPE_REMOTE.equalsIgnoreCase(scope)) {
                // 暴露服务到本地，即用injvm协议进行协议暴露
                exportLocal(url);
            }

            if (!SCOPE_LOCAL.equalsIgnoreCase(scope)) {
                if (CollectionUtils.isNotEmpty(registryURLs)) {
                    // 将服务注册到多个注册中心上去
                    for (URL registryURL : registryURLs) {
                        //if protocol is only injvm ,not register
                        if (LOCAL_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
                            continue;
                        }
                        url = url.addParameterIfAbsent(DYNAMIC_KEY, registryURL.getParameter(DYNAMIC_KEY));
                        URL monitorUrl = ConfigValidationUtils.loadMonitor(this, registryURL);
                        if (monitorUrl != null) {
                            url = url.addParameterAndEncoded(MONITOR_KEY, monitorUrl.toFullString());
                        }
                        if (logger.isInfoEnabled()) {
                            if (url.getParameter(REGISTER_KEY, true)) {
                                logger.info("Register dubbo service " + interfaceClass.getName() + " url " + url + " to registry " + registryURL);
                            } else {
                                logger.info("Export dubbo service " + interfaceClass.getName() + " to url " + url);
                            }
                        }

                        // For providers, this is used to enable custom proxy to generate invoker
                        String proxy = url.getParameter(PROXY_KEY);
                        if (StringUtils.isNotEmpty(proxy)) {
                            registryURL = registryURL.addParameter(PROXY_KEY, proxy);
                        }

                        registryURL = registryURL.addParameterAndEncoded(EXPORT_KEY, url.toFullString());

                        /**
                         * 创建真实服务的代理对象Invoker，此处并没有使用jdk动态代理来生成代理对象，而是直接通过Invoker
                         * 来包装真实对象，并充当代理对象。
                         *
                         * PROXY_FACTORY可以参考{@link org.apache.dubbo.rpc.proxy.jdk.JdkProxyFactory}
                         *
                         * 返回的是包含{@link java.util.concurrent.CompletableFuture<AppResponse >}的 {@link org.apache.dubbo.rpc.AsyncRpcResult}。
                         *
                         */
                        Invoker<?> invoker = PROXY_FACTORY.getInvoker(ref, (Class) interfaceClass, registryURL);

                        /**
                         * 用于包装Invoker与ServiceConfig
                         */
                        DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);

                        /**
                         * 将真实服务的的代理Invoker暴露到对应的协议服务端上并将服务端信息注册到注册中心，此处的URL协议为registry，
                         * 由于Dubbo SPI的AOP机制，此处的调用链路为
                         *
                         * {@link org.apache.dubbo.rpc.Protocol$Adaptive#export(Invoker)}                  =>
                         * {@link org.apache.dubbo.rpc.protocol.ProtocolListenerWrapper#export(Invoker)}   =>
                         * {@link org.apache.dubbo.rpc.protocol.ProtocolFilterWrapper#export(Invoker)}     =>
                         * {@link org.apache.dubbo.registry.integration.RegistryProtocol#export(Invoker)}
                         *
                         */
                        Exporter<?> exporter = protocol.export(wrapperInvoker);
                        exporters.add(exporter);
                    }
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("Export dubbo service " + interfaceClass.getName() + " to url " + url);
                    }
                    Invoker<?> invoker = PROXY_FACTORY.getInvoker(ref, (Class) interfaceClass, url);
                    DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);

                    Exporter<?> exporter = protocol.export(wrapperInvoker);
                    exporters.add(exporter);
                }
                /**
                 * @since 2.7.0
                 * ServiceData Store
                 */
                WritableMetadataService metadataService = WritableMetadataService.getExtension(url.getParameter(METADATA_KEY, DEFAULT_METADATA_STORAGE_TYPE));
                if (metadataService != null) {
                    metadataService.publishServiceDefinition(url);
                }
            }
        }
        this.urls.add(url);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    /**
     * always export injvm
     */
    private void exportLocal(URL url) {
        // local => injvm://127.0.0.1:0/....
        URL local = URLBuilder.from(url)
                .setProtocol(LOCAL_PROTOCOL)
                .setHost(LOCALHOST_VALUE)
                .setPort(0)
                .build();
        Exporter<?> exporter = protocol.export(
                PROXY_FACTORY.getInvoker(ref, (Class) interfaceClass, local));
        exporters.add(exporter);
        logger.info("Export dubbo service " + interfaceClass.getName() + " to local registry url : " + local);
    }

    /**
     * Determine if it is injvm
     *
     * @return
     */
    private boolean isOnlyInJvm() {
        return getProtocols().size() == 1
                && LOCAL_PROTOCOL.equalsIgnoreCase(getProtocols().get(0).getName());
    }


    /**
     * Register & bind IP address for service provider, can be configured separately.
     * Configuration priority: environment variables -> java system properties -> host property in config file ->
     * /etc/hosts -> default network address -> first available network address
     *
     * @param protocolConfig
     * @param registryURLs
     * @param map
     * @return
     */
    private String findConfigedHosts(ProtocolConfig protocolConfig,
                                     List<URL> registryURLs,
                                     Map<String, String> map) {
        boolean anyhost = false;

        String hostToBind = getValueFromConfig(protocolConfig, DUBBO_IP_TO_BIND);
        if (hostToBind != null && hostToBind.length() > 0 && isInvalidLocalHost(hostToBind)) {
            throw new IllegalArgumentException("Specified invalid bind ip from property:" + DUBBO_IP_TO_BIND + ", value:" + hostToBind);
        }

        // if bind ip is not found in environment, keep looking up
        if (StringUtils.isEmpty(hostToBind)) {
            hostToBind = protocolConfig.getHost();
            if (provider != null && StringUtils.isEmpty(hostToBind)) {
                hostToBind = provider.getHost();
            }
            if (isInvalidLocalHost(hostToBind)) {
                anyhost = true;
                try {
                    logger.info("No valid ip found from environment, try to find valid host from DNS.");
                    hostToBind = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    logger.warn(e.getMessage(), e);
                }
                if (isInvalidLocalHost(hostToBind)) {
                    if (CollectionUtils.isNotEmpty(registryURLs)) {
                        for (URL registryURL : registryURLs) {
                            if (MULTICAST.equalsIgnoreCase(registryURL.getParameter("registry"))) {
                                // skip multicast registry since we cannot connect to it via Socket
                                continue;
                            }
                            try (Socket socket = new Socket()) {
                                SocketAddress addr = new InetSocketAddress(registryURL.getHost(), registryURL.getPort());
                                socket.connect(addr, 1000);
                                hostToBind = socket.getLocalAddress().getHostAddress();
                                break;
                            } catch (Exception e) {
                                logger.warn(e.getMessage(), e);
                            }
                        }
                    }
                    if (isInvalidLocalHost(hostToBind)) {
                        hostToBind = getLocalHost();
                    }
                }
            }
        }

        // 服务端的IP地址
        map.put(BIND_IP_KEY, hostToBind);

        // registry ip is not used for bind ip by default
        String hostToRegistry = getValueFromConfig(protocolConfig, DUBBO_IP_TO_REGISTRY);
        if (hostToRegistry != null && hostToRegistry.length() > 0 && isInvalidLocalHost(hostToRegistry)) {
            throw new IllegalArgumentException("Specified invalid registry ip from property:" + DUBBO_IP_TO_REGISTRY + ", value:" + hostToRegistry);
        } else if (StringUtils.isEmpty(hostToRegistry)) {
            // bind ip is used as registry ip by default
            hostToRegistry = hostToBind;
        }

        map.put(ANYHOST_KEY, String.valueOf(anyhost));

        return hostToRegistry;
    }


    /**
     * Register port and bind port for the provider, can be configured separately
     * Configuration priority: environment variable -> java system properties -> port property in protocol config file
     * -> protocol default port
     *
     * @param protocolConfig
     * @param name
     * @return
     */
    private Integer findConfigedPorts(ProtocolConfig protocolConfig,
                                      String name,
                                      Map<String, String> map) {
        Integer portToBind = null;

        // parse bind port from environment
        String port = getValueFromConfig(protocolConfig, DUBBO_PORT_TO_BIND);
        portToBind = parsePort(port);

        // if there's no bind port found from environment, keep looking up.
        if (portToBind == null) {
            portToBind = protocolConfig.getPort();
            if (provider != null && (portToBind == null || portToBind == 0)) {
                portToBind = provider.getPort();
            }
            final int defaultPort = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(name).getDefaultPort();
            if (portToBind == null || portToBind == 0) {
                portToBind = defaultPort;
            }
            if (portToBind == null || portToBind <= 0) {
                portToBind = getRandomPort(name);
                if (portToBind == null || portToBind < 0) {
                    portToBind = getAvailablePort(defaultPort);
                    putRandomPort(name, portToBind);
                }
            }
        }

        // save bind port, used as url's key later
        map.put(BIND_PORT_KEY, String.valueOf(portToBind));

        // registry port, not used as bind port by default
        String portToRegistryStr = getValueFromConfig(protocolConfig, DUBBO_PORT_TO_REGISTRY);
        Integer portToRegistry = parsePort(portToRegistryStr);
        if (portToRegistry == null) {
            portToRegistry = portToBind;
        }

        return portToRegistry;
    }

    private Integer parsePort(String configPort) {
        Integer port = null;
        if (configPort != null && configPort.length() > 0) {
            try {
                Integer intPort = Integer.parseInt(configPort);
                if (isInvalidPort(intPort)) {
                    throw new IllegalArgumentException("Specified invalid port from env value:" + configPort);
                }
                port = intPort;
            } catch (Exception e) {
                throw new IllegalArgumentException("Specified invalid port from env value:" + configPort);
            }
        }
        return port;
    }

    private String getValueFromConfig(ProtocolConfig protocolConfig, String key) {
        String protocolPrefix = protocolConfig.getName().toUpperCase() + "_";
        String port = ConfigUtils.getSystemProperty(protocolPrefix + key);
        if (StringUtils.isEmpty(port)) {
            port = ConfigUtils.getSystemProperty(key);
        }
        return port;
    }

    private Integer getRandomPort(String protocol) {
        protocol = protocol.toLowerCase();
        return RANDOM_PORT_MAP.getOrDefault(protocol, Integer.MIN_VALUE);
    }

    private void putRandomPort(String protocol, Integer port) {
        protocol = protocol.toLowerCase();
        if (!RANDOM_PORT_MAP.containsKey(protocol)) {
            RANDOM_PORT_MAP.put(protocol, port);
            logger.warn("Use random available port(" + port + ") for protocol " + protocol);
        }
    }

    public void appendParameters() {
        URL appendParametersUrl = URL.valueOf("appendParameters://");
        List<AppendParametersComponent> appendParametersComponents = ExtensionLoader.getExtensionLoader(AppendParametersComponent.class).getActivateExtension(appendParametersUrl, (String[]) null);
        appendParametersComponents.forEach(component -> component.appendExportParameters(this));
    }

    /**
     * Dispatch an {@link Event event}
     *
     * @param event an {@link Event event}
     * @since 2.7.5
     */
    private void dispatch(Event event) {
        EventDispatcher.getDefaultExtension().dispatch(event);
    }

    public DubboBootstrap getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(DubboBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }
}