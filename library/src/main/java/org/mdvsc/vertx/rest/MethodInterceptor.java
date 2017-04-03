package org.mdvsc.vertx.rest;

import io.vertx.ext.web.RoutingContext;

/**
 * @author HanikLZ
 * @since 2017/3/8
 */
public interface MethodInterceptor {

    /**
     * before method invoke
     * @param context routing context
     * @param caller method caller
     **/
    void intercept(RoutingContext context, MethodCaller caller);

}

