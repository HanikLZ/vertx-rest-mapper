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

    public void endWithCall() {
        if (methodCache.isBlocking()) { // to block if method has return
            context.vertx().executeBlocking(fut -> {
                endWithCallImpl();
                fut.complete();
            }, methodCache.isOrderBlocking(), res -> {
                if (res.failed()) {
                    context.fail(res.cause());
                }
            });
        } else {
            endWithCallImpl();
        }
        ended = true;
    }

    public void endWithFail(HttpResponseStatus status) {
        endWithFail(status.code());
    }

    public void endWithFail(int code) {
        context.fail(code);
        ended = true;
    }

    public void endWithFail(Throwable e) {
        context.fail(e);
        ended = true;
    }

    public boolean isEnded() {
        return ended;
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
        if (!response.ended()) {
           if (result == null) response.end(); else response.end(serializer.serialize(result));
        }
        ended = true;
    }

}

