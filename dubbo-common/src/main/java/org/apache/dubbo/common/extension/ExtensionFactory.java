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
package org.apache.dubbo.common.extension;

/**
 * 加载SPI实现的工厂，最终通过该工厂来获取具体SPI的实现
 * <p>
 * ExtensionFactory
 */
@SPI
public interface ExtensionFactory {

    /**
     * <pre>
     * 根据SPI接口类型与名称来加载SPI实现，从而达到了按需加载的功能。
     * 因为dubbo自己实现的一套SPI机制，在dubbo SPI中每个实现都有一个
     * 唯一的名字，所以在SPI描述文件中并不是简单的value格式，而是
     * key-value的存储，key为对应的实现名称，value为对应的SPI实现
     * </pre>
     *
     * @param type object type.
     * @param name object name.
     * @return object instance.
     */
    <T> T getExtension(Class<T> type, String name);

}
