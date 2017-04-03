package org.mdvsc.vertx.rest;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.mdvsc.vertx.ResponseConstants;

/**
 * @author HanikLZ
 * @since 2017/4/2
 */
public class MethodCaller {

    private final MethodCache methodCache;
    private final Object caller;
    private final Object[] arguments;
    private final HttpServerResponse response;
    private final Serializer serializer;

    MethodCaller(MethodCache methodCache, Object caller, Object[] arguments, HttpServerResponse response, Serializer serializer) {
        this.methodCache = methodCache;
        this.caller = caller;
        this.arguments = arguments;
        this.response = response;
        this.serializer = serializer;
    }

    public HttpServerResponse getResponse() {
        return response;
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

    public void endWithCall(final RoutingContext context) {
        if (methodCache.hasReturn()) { // to block if method has return
            context.vertx().executeBlocking(fut -> {
                endWithCall(context, true);
                fut.complete();
            }, false, res -> {
                if (res.failed()) {
                    context.fail(res.cause());
                }
            });
        } else {
            endWithCall(context, false);
        }
    }

    private void endWithCall(final RoutingContext context, boolean hasReturn) {
        Object result;
        try {
            result = methodCache.getMethod().invoke(caller, arguments);
        } catch (Exception exception) {
            result = exception;
        }
        if (result instanceof Throwable) {
            context.fail((Throwable) result);
        } else if (hasReturn) {
            response.setStatusCode(ResponseConstants.STATUS_CODE_OK);
            if (result == null) response.end(); else response.end(serializer.serialize(result));
        }
    }

}

