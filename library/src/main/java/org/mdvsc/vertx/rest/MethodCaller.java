package org.mdvsc.vertx.rest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

import static io.vertx.core.http.HttpHeaders.CONTENT_LENGTH;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

/**
 * @author HanikLZ
 * @since 2017/4/2
 */
public class MethodCaller implements Handler<Void> {

    private final MethodCache methodCache;
    private final Object caller;
    private final Object[] arguments;
    private final RoutingContext context;
    private final Serializer serializer;

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
        if (methodCache.hasReturn()) { // to block if method has return
            context.vertx().executeBlocking(fut -> {
                endWithCall(true);
                fut.complete();
            }, false, res -> {
                if (res.failed()) {
                    context.fail(res.cause());
                }
            });
        } else {
            endWithCall( false);
        }
    }


    public void endWithFail(HttpResponseStatus status, Throwable e) {
        endWithFail(status.code());
        endWithFail(e);
    }

    public void endWithFail(HttpResponseStatus status) {
        endWithFail(status.code());
    }

    public void endWithFail(int code) {
        context.fail(code);
    }

    public void endWithFail(Throwable e) {
        context.fail(e);
    }

    @Override
    public void handle(Void event) {
        String acceptableContentType = context.getAcceptableContentType();
        if (acceptableContentType == null) acceptableContentType = serializer.mediaType();
        MultiMap headers = context.response().headers();
        if (!headers.contains(CONTENT_TYPE) && !"0".equals(headers.get(CONTENT_LENGTH))) {
            headers.add(CONTENT_TYPE, acceptableContentType + ";charset=" + serializer.mediaEncode());
        }
    }

    private void endWithCall(boolean hasReturn) {
        Object result;
        try {
            result = methodCache.getMethod().invoke(caller, arguments);
        } catch (Exception exception) {
            result = exception;
        }
        if (result instanceof Throwable) {
            endWithFail((Throwable) result);
        } else if (hasReturn) {
            if (result == null) context.response().end(); else context.response().end(serializer.serialize(result));
        }
    }

}

