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

package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.transport.AbstractChannelHandlerDelegate;

import static org.apache.dubbo.common.constants.CommonConstants.HEARTBEAT_EVENT;

public class HeartbeatHandler extends AbstractChannelHandlerDelegate {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);

    public static final String KEY_READ_TIMESTAMP = "READ_TIMESTAMP";

    public static final String KEY_WRITE_TIMESTAMP = "WRITE_TIMESTAMP";

    public HeartbeatHandler(ChannelHandler handler) {
        /**
         * 【DispatcherChannelHandler：扩展点】--->DecodeHandler--->HeaderExchangeHandler--->ExchangeHandlerAdapter
         */
        super(handler);
    }

    @Override
    public void connected(Channel channel) throws RemotingException {
        setReadTimestamp(channel);
        setWriteTimestamp(channel);
        /**
         * {@link org.apache.dubbo.remoting.transport.dispatcher.all.AllChannelHandler#connected(Channel)}
         * {@link org.apache.dubbo.remoting.transport.dispatcher.direct.DirectChannelHandler#connected(Channel)}
         * {@link org.apache.dubbo.remoting.transport.dispatcher.connection.ConnectionOrderedChannelHandler#connected(Channel)}
         * {@link org.apache.dubbo.remoting.transport.dispatcher.execution.ExecutionChannelHandler#connected(Channel)}
         * {@link org.apache.dubbo.remoting.transport.dispatcher.message.MessageOnlyChannelHandler#connected(Channel)}
         */
        handler.connected(channel);
    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
        clearReadTimestamp(channel);
        clearWriteTimestamp(channel);
        /**
         * {@link org.apache.dubbo.remoting.transport.dispatcher.all.AllChannelHandler#disconnected(Channel)}
         * {@link org.apache.dubbo.remoting.transport.dispatcher.direct.DirectChannelHandler#disconnected(Channel)}
         * {@link org.apache.dubbo.remoting.transport.dispatcher.connection.ConnectionOrderedChannelHandler#disconnected(Channel)}
         * {@link org.apache.dubbo.remoting.transport.dispatcher.execution.ExecutionChannelHandler#disconnected(Channel)}
         * {@link org.apache.dubbo.remoting.transport.dispatcher.message.MessageOnlyChannelHandler#disconnected(Channel)}
         */
        handler.disconnected(channel);
    }

    @Override
    public void sent(Channel channel, Object message) throws RemotingException {
        setWriteTimestamp(channel);
        /**
         * {@link org.apache.dubbo.remoting.transport.dispatcher.all.AllChannelHandler#sent(Channel, Object)}
         * {@link org.apache.dubbo.remoting.transport.dispatcher.direct.DirectChannelHandler#sent(Channel, Object)}
         * {@link org.apache.dubbo.remoting.transport.dispatcher.connection.ConnectionOrderedChannelHandler#sent(Channel, Object)}
         * {@link org.apache.dubbo.remoting.transport.dispatcher.execution.ExecutionChannelHandler#sent(Channel, Object)}
         * {@link org.apache.dubbo.remoting.transport.dispatcher.message.MessageOnlyChannelHandler#sent(Channel, Object)}
         */
        handler.sent(channel, message);
    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        setReadTimestamp(channel);
        if (isHeartbeatRequest(message)) {
            Request req = (Request) message;
            if (req.isTwoWay()) {
                Response res = new Response(req.getId(), req.getVersion());
                res.setEvent(HEARTBEAT_EVENT);
                channel.send(res);
                if (logger.isInfoEnabled()) {
                    int heartbeat = channel.getUrl().getParameter(Constants.HEARTBEAT_KEY, 0);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Received heartbeat from remote channel " + channel.getRemoteAddress()
                                + ", cause: The channel has no data-transmission exceeds a heartbeat period"
                                + (heartbeat > 0 ? ": " + heartbeat + "ms" : ""));
                    }
                }
            }
            return;
        }
        if (isHeartbeatResponse(message)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Receive heartbeat response in thread " + Thread.currentThread().getName());
            }
            return;
        }
        /**
         * {@link org.apache.dubbo.remoting.transport.dispatcher.all.AllChannelHandler#received(Channel, Object)}
         * {@link org.apache.dubbo.remoting.transport.dispatcher.direct.DirectChannelHandler#received(Channel, Object)}
         * {@link org.apache.dubbo.remoting.transport.dispatcher.connection.ConnectionOrderedChannelHandler#received(Channel, Object)}
         * {@link org.apache.dubbo.remoting.transport.dispatcher.execution.ExecutionChannelHandler#received(Channel, Object)}
         * {@link org.apache.dubbo.remoting.transport.dispatcher.message.MessageOnlyChannelHandler#received(Channel, Object)}
         */
        handler.received(channel, message);
    }

    private void setReadTimestamp(Channel channel) {
        channel.setAttribute(KEY_READ_TIMESTAMP, System.currentTimeMillis());
    }

    private void setWriteTimestamp(Channel channel) {
        channel.setAttribute(KEY_WRITE_TIMESTAMP, System.currentTimeMillis());
    }

    private void clearReadTimestamp(Channel channel) {
        channel.removeAttribute(KEY_READ_TIMESTAMP);
    }

    private void clearWriteTimestamp(Channel channel) {
        channel.removeAttribute(KEY_WRITE_TIMESTAMP);
    }

    private boolean isHeartbeatRequest(Object message) {
        return message instanceof Request && ((Request) message).isHeartbeat();
    }

    private boolean isHeartbeatResponse(Object message) {
        return message instanceof Response && ((Response) message).isHeartbeat();
    }
}
