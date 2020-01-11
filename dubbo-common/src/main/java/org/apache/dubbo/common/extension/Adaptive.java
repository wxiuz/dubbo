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

import org.apache.dubbo.common.URL;

import java.lang.annotation.*;

/**
 * 主要用于SPI之间的依赖注入，一个SPI实现依赖于另外一个SPI，此时注入的到该SPI的实现类是另外一个SPI的Adaptive实现，
 * 而SPI在调用的时候再根据@Adaptive属性来指定从URL中获取某个参数来决定获取哪个具体的实现
 * <p>
 * <p>
 * Provide helpful information for {@link ExtensionLoader} to inject dependency extension instance.
 *
 * @see ExtensionLoader
 * @see URL
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Adaptive {
    
    /**
     * 指定URL中的参数名称，根据该参数名称来获取参数的值，然后该值作为具体SPI实现的名称
     *
     * @return parameter names in URL
     */
    String[] value() default {};

}