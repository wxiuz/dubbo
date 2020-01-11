package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

/**
 * @Auther: wuxiuzhao
 * @Date: 2020/1/11 15:18
 * @Description:
 */
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER})
public class LogFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        System.err.println("+++++++++++++++++++++++++++++++Log Filter +++++++++++++++++++++++++++++++++++");
        return invoker.invoke(invocation);
    }
}
