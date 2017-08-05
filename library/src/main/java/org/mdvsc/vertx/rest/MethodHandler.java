package org.mdvsc.vertx.rest;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

import java.lang.reflect.Method;
import java.util.*;

import static io.vertx.core.http.HttpHeaders.CONTENT_LENGTH;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

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

        final Map<Class, Object> map = new HashMap<>();
        map.put(RoutingContext.class, event);
        map.put(HttpServerResponse.class, event.response());
        map.put(HttpServerRequest.class, event.request());

        final Serializer serializer = contextProvider.provideContext(Serializer.class);
        final RequestFilter requestFilter = contextProvider.provideContext(RequestFilter.class);
        final ResponseFilter responseFilter = contextProvider.provideContext(ResponseFilter.class);
        if (requestFilter != null) requestFilter.filter(event.request());
        for (MethodCache cache : handleMethods) {
            final Object[] args;
            try {
                args = cache.buildInvokeArgs(event, serializer, contextProvider, map);
            } catch (Exception ignored) {
                // ignored
                continue;
            }
            final Object resourceInstance = contextProvider.provideContext(resourceClass);
            final MethodInterceptor methodInterceptor = contextProvider.provideContext(MethodInterceptor.class);
            MethodCaller methodCaller = new MethodCaller(cache, resourceInstance, args, event, serializer);
            event.response().headersEndHandler(e -> addDefaultResponseHeader(event, serializer, responseFilter));
            if (methodInterceptor != null) methodInterceptor.intercept(methodCaller);
            if (!methodCaller.isEnded()) methodCaller.endWithCall();
            return;
        }

        event.next();

    }

    private void addDefaultResponseHeader(RoutingContext context, Serializer serializer, ResponseFilter responseFilter) {
        String acceptableContentType = context.getAcceptableContentType();
        if (acceptableContentType == null) acceptableContentType = serializer.mediaType();
        HttpServerResponse response = context.response();
        MultiMap headers = response.headers();
        if (!headers.contains(CONTENT_TYPE) && !"0".equals(headers.get(CONTENT_LENGTH))) {
            headers.add(CONTENT_TYPE, acceptableContentType + ";charset=" + serializer.mediaEncode());
        }
        if (responseFilter != null) {
            responseFilter.filter(context.request(), response);
        }
    }

}

