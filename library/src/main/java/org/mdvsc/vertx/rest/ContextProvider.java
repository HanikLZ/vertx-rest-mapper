package org.mdvsc.vertx.rest;

/**
 * @author HanikLZ
 * @since 2017/3/8
 */
public interface ContextProvider {
    <T> T provideContext(Class<T> clz);
}

