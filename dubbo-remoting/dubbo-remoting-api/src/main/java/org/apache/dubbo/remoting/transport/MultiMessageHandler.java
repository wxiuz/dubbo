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
package org.apache.dubbo.remoting.transport;

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.support.MultiMessage;

/**
 * 事件处理器代理，该代理可以一次接收多个请求，并将所有请求转到实际的事件处理器
 *
 * @see MultiMessage
 */
public class MultiMessageHandler extends AbstractChannelHandlerDelegate {

    public MultiMessageHandler(ChannelHandler handler) {
        super(handler);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        if (message instanceof MultiMessage) {
            // 如果收到的消息为MultiMessage，则需要将消息拆分为单个的消息去处理
            MultiMessage list = (MultiMessage) message;
            for (Object obj : list) {
                // 交由实际的事件处理器进行处理
                handler.received(channel, obj);
            }
        } else {
            // 如果收到的消息就是单个消息，则直接交由处理器处理
            handler.received(channel, message);
        }
    }
}
