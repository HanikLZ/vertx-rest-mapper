package org.mdvsc.vertx.rest;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.mdvsc.vertx.ResponseConstants;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Vertx route handler
 */
class MethodHandler implements Handler<RoutingContext> {

    private final List<MethodCache> handleMethods = new ArrayList<>();
    private final Class resourceClass;
    private final ContextProvider contextProvider;

    MethodHandler(Class resourceClass, ContextProvider provider) {
        this.resourceClass = resourceClass;
        this.contextProvider = provider;
    }

    /**
     * add method to this handler
     * @param method method
     * @param methodComparator comparator, nullable
     * @return this
     */
    MethodHandler addHandleMethod(Method method, Comparator<MethodCache> methodComparator) {
        handleMethods.add(new MethodCache(method));
        if (methodComparator != null) handleMethods.sort(methodComparator);
        return this;
    }

    @Override
    public void handle(RoutingContext event) {

        final Object resourceInstance = contextProvider.provideContext(resourceClass);
        final Serializer serializer = contextProvider.provideContext(Serializer.class);
        final MethodInterceptor methodInterceptor = contextProvider.provideContext(MethodInterceptor.class);
        final HttpServerResponse response = event.response().putHeader(MediaType.CONTENT_TYPE, serializer.mediaType() + ";charset=" + serializer.mediaEncode());
        final Map<Class, Object> map = new HashMap<>();

        map.put(RoutingContext.class, event);
        map.put(HttpServerResponse.class, response);

        for (MethodCache cache : handleMethods) {
            final Object[] args;
            try {
                args = cache.buildInvokeArgs(event, serializer, contextProvider, map);
            } catch (Exception ignored) {
                // ignored
                continue;
            }
            MethodCaller methodCaller = new MethodCaller(cache, resourceInstance, args, response, serializer);
            if (methodInterceptor != null) {
                methodInterceptor.intercept(event, methodCaller);
            } else {
                methodCaller.endWithCall(event);
            }
            return;
        }

        event.fail(ResponseConstants.STATUS_CODE_BAD_REQUEST);
    }

}

