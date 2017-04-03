package org.mdvsc.vertx.rest.sample;

import io.vertx.core.Vertx;

/**
 * @author HanikLZ
 * @since 2017/4/2
 */
public class RestSampleMain {

    public static void main(String... args) {
        Vertx.vertx().deployVerticle(new MyRestServer());
    }

}

