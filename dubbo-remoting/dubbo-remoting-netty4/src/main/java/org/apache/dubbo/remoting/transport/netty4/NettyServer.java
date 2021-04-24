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
package org.apache.dubbo.remoting.transport.netty4;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.*;
import org.apache.dubbo.remoting.transport.AbstractServer;
import org.apache.dubbo.remoting.transport.dispatcher.ChannelHandlers;
import org.apache.dubbo.remoting.utils.UrlUtils;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.dubbo.common.constants.CommonConstants.IO_THREADS_KEY;
import static org.apache.dubbo.remoting.Constants.SSL_ENABLED_KEY;

/**
 * NettyServer.
 *
 * @author
 */
public class NettyServer extends AbstractServer implements RemotingServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
    /**
     * 一个channel代表一个客户端的连接，记录了所有客户端的连接
     * <ip:port, dubbo channel>
     */
    private Map<String, Channel> channels;

    /**
     * 原生的Netty server bootstrap.
     */
    private ServerBootstrap bootstrap;

    /**
     * Netty的BossReactor对应的原生Netty Channel，接收请求并将连接转发到Worker线程
     */
    private io.netty.channel.Channel channel;

    /**
     * Netty的Reactor模型的BossReactor，用于接收客户端的连接请求【不处理数据读写请求】，一般就是一个线程【一个多路复用器Selector】
     */
    private EventLoopGroup bossGroup;

    /**
     * Netty的Reactor模型的WorkerReactor，用于接收客户端的读写请求【不处理数据连接请求】，一般就是一个线程【一个多路复用器Selector】，
     * 此处就是通过IoThreads来配置该线程数
     */
    private EventLoopGroup workerGroup;

    /**
     * 创建Netty Server
     *
     * @param url
     * @param handler 事件处理器
     * @throws RemotingException
     */
    public NettyServer(URL url, ChannelHandler handler) throws RemotingException {
        super(ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME), ChannelHandlers.wrap(handler, url));
    }

    /**
     * 启动Netty服务
     *
     * @throws Throwable
     */
    @Override
    protected void doOpen() throws Throwable {
        // 创建Netty ServerBootstrap
        bootstrap = new ServerBootstrap();

        // Reactor模型
        // Netty mainReactor：接收连接请求---->Selector
        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("NettyServerBoss", true));
        // Netty subReactor：处理IO读写请求---->Selector
        workerGroup = new NioEventLoopGroup(getUrl().getPositiveParameter(IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS),
                new DefaultThreadFactory("NettyServerWorker", true));

        /**
         * NettyServerHandler是Netty的原生的Handler的实现，用于处理Netty的事件
         */
        final NettyServerHandler nettyServerHandler = new NettyServerHandler(getUrl(), this);
        channels = nettyServerHandler.getChannels();

        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    /**
                     * 每当一个新的连接进来，则会调用该方法来配置新建立的连接NioSocketChannel
                     * @param ch
                     * @throws Exception
                     */
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        // FIXME: should we use getTimeout()?
                        int idleTimeout = UrlUtils.getIdleTimeout(getUrl());
                        NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyServer.this);
                        if (getUrl().getParameter(SSL_ENABLED_KEY, false)) {
                            ch.pipeline().addLast("negotiation", SslHandlerInitializer.sslServerHandler(getUrl(), nettyServerHandler));
                        }
                        // 定义HandlerPipeline
                        ch.pipeline()
                                // 定义消息解码器（接收消息时用）
                                .addLast("decoder", adapter.getDecoder())
                                // 定义消息编码器（发送消息时用）
                                .addLast("encoder", adapter.getEncoder())
                                .addLast("server-idle-handler", new IdleStateHandler(0, 0, idleTimeout, MILLISECONDS))
                                // 定义Netty的事件处理器
                                .addLast("handler", nettyServerHandler);
                    }
                });
        // bind
        ChannelFuture channelFuture = bootstrap.bind(getBindAddress());
        channelFuture.syncUninterruptibly();
        channel = channelFuture.channel();

    }

    @Override
    protected void doClose() throws Throwable {
        try {
            if (channel != null) {
                // unbind.
                channel.close();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            Collection<org.apache.dubbo.remoting.Channel> channels = getChannels();
            if (channels != null && channels.size() > 0) {
                for (org.apache.dubbo.remoting.Channel channel : channels) {
                    try {
                        channel.close();
                    } catch (Throwable e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (bootstrap != null) {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (channels != null) {
                channels.clear();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public Collection<Channel> getChannels() {
        Collection<Channel> chs = new HashSet<Channel>();
        for (Channel channel : this.channels.values()) {
            if (channel.isConnected()) {
                chs.add(channel);
            } else {
                channels.remove(NetUtils.toAddressString(channel.getRemoteAddress()));
            }
        }
        return chs;
    }

    @Override
    public Channel getChannel(InetSocketAddress remoteAddress) {
        return channels.get(NetUtils.toAddressString(remoteAddress));
    }

    @Override
    public boolean canHandleIdle() {
        return true;
    }

    @Override
    public boolean isBound() {
        return channel.isActive();
    }

}
