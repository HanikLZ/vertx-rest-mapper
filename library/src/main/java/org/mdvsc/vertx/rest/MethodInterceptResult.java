package org.mdvsc.vertx.rest;

import io.vertx.core.AsyncResult;

/**
 * @author HanikLZ
 * @since 2017/4/2
 */
public class MethodInterceptResult implements AsyncResult<Object> {

    public static MethodInterceptResult success(Object result) {
        return new MethodInterceptResult(true, result, null);
    }

    public static MethodInterceptResult fail(Throwable cause) {
        return new MethodInterceptResult(false, null, cause);
    }

    private final Object result;
    private final Throwable cause;
    private final boolean succeeded;

    public MethodInterceptResult(boolean succeeded, Object result, Throwable cause) {
        this.succeeded = succeeded;
        this.result = result;
        this.cause = cause;
    }

    @Override
    public Object result() {
        return result;
    }

    @Override
    public Throwable cause() {
        return cause;
    }

    @Override
    public boolean succeeded() {
        return succeeded;
    }

    @Override
    public boolean failed() {
        return !succeeded;
    }

}
