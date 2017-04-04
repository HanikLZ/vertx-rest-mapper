package org.mdvsc.vertx.rest;

/**
 * @author HanikLZ
 * @since 2017/3/8
 */
public interface MethodInterceptor {

    /**
     * before method invoke
     * @param caller method caller
     **/
    void intercept(MethodCaller caller);

}

