package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.*;

/**
 * @author lenovo
 */
public class FilterWrapperInvoker<T> implements Invoker<T> {

    private Invoker<T> originInvoker;

    private Invoker<T> nextInvoker;

    private Filter filter;

    public FilterWrapperInvoker(Invoker<T> originInvoker, Invoker<T> nextInvoker, Filter filter) {
        this.originInvoker = originInvoker;
        this.nextInvoker = nextInvoker;
        this.filter = filter;
    }

    @Override
    public URL getUrl() {
        return originInvoker.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return originInvoker.isAvailable();
    }

    @Override
    public void destroy() {
        originInvoker.destroy();
    }

    @Override
    public Class<T> getInterface() {
        return originInvoker.getInterface();
    }


    @Override
    public String toString() {
        return originInvoker.toString();
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        Result asyncResult;
        try {
            /**
             * 返回的是包含{@link java.util.concurrent.CompletableFuture<AppResponse >}的 {@link org.apache.dubbo.rpc.AsyncRpcResult}。
             */
            asyncResult = filter.invoke(nextInvoker, invocation);
        } catch (Exception e) {
            // Deprecated!
            if (filter instanceof ListenableFilter) {
                Filter.Listener listener = ((ListenableFilter) filter).listener();
                if (listener != null) {
                    listener.onError(e, originInvoker, invocation);
                }
            } else if (filter instanceof Filter.Listener) {
                Filter.Listener listener = (Filter.Listener) filter;
                listener.onError(e, originInvoker, invocation);
            }
            throw e;
        } finally {

        }
        return asyncResult.whenCompleteWithContext((r, t) -> {
            if (filter instanceof ListenableFilter) {// Deprecated!
                Filter.Listener listener = ((ListenableFilter) filter).listener();
                if (listener != null) {
                    if (t == null) {
                        listener.onMessage(r, originInvoker, invocation);
                    } else {
                        listener.onError(t, originInvoker, invocation);
                    }
                }
            } else if (filter instanceof Filter.Listener) {
                Filter.Listener listener = (Filter.Listener) filter;
                if (t == null) {
                    listener.onMessage(r, originInvoker, invocation);
                } else {
                    listener.onError(t, originInvoker, invocation);
                }
            }
        });
    }
}
