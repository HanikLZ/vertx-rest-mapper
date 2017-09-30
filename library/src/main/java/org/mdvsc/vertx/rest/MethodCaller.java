package org.mdvsc.vertx.rest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * @author HanikLZ
 * @since 2017/4/2
 */
public class MethodCaller {

    private final MethodCache methodCache;
    private final Object caller;
    private final Object[] arguments;
    private final RoutingContext context;
    private final Serializer serializer;

    private boolean ended = false;

    MethodCaller(MethodCache methodCache, Object caller, Object[] arguments, RoutingContext context, Serializer serializer) {
        this.methodCache = methodCache;
        this.caller = caller;
        this.arguments = arguments;
        this.context = context;
        this.serializer = serializer;
    }

    public RoutingContext getContext() {
        return context;
    }

    public Object getCaller() {
        return caller;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public MethodCache getMethodCache() {
        return methodCache;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public void setEnd() {
        ended = true;
    }

    public boolean isEnded() {
        return ended;
    }

    public void endWithCall() {
        if (methodCache.isBlocking()) { // to block if method has return
            context.vertx().executeBlocking(fut -> {
                endWithCallImpl();
                fut.complete();
            }, methodCache.isOrderBlocking(), res -> {
                if (res.failed() && !context.failed()) {
                    context.fail(res.cause());
                }
            });
        } else {
            endWithCallImpl();
        }
        setEnd();
    }

    public void endWithFail(HttpResponseStatus status) {
        endWithFail(status.code());
    }

    public void endWithFail(int code) {
        if (!context.failed()) context.fail(code);
        setEnd();
    }

    public void endWithFail(Throwable e) {
        if (!context.failed()) context.fail(e);
        setEnd();
    }

    private void endWithCallImpl() {
        HttpServerResponse response = context.response();
        Object result;
        try {
            result = methodCache.getMethod().invoke(caller, arguments);
        } catch (Exception exception) {
            if (!context.failed()) {
                context.fail(exception);
            }
            return;
        }
        if (!methodCache.isHandleEnd() && !response.ended()) {
           if (result == null) response.end(); else response.end(serializer.serialize(result));
        }
        setEnd();
    }

}

