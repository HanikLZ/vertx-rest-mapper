package org.mdvsc.vertx.rest.sample;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.mdvsc.vertx.rest.*;

import java.util.stream.Stream;

/**
 * @author HanikLZ
 * @since 2017/4/2
 */
public class MyRestServer extends SimpleRestServer implements MethodInterceptor {

    public MyRestServer(MyRestServer.Options options) {
        super(options);
        restRouteMapper.addContextClass(SampleResource.class, RootResource.class);
        restRouteMapper.setMethodInterceptor(this);
    }

    @Override
    public void intercept(MethodCaller caller) {
        if (Stream.of(caller.getMethodCache().getAnnotations()).anyMatch(annotation -> annotation instanceof TestFilter)) {
            caller.endWithCall();
        } else {
            caller.endWithFail(HttpResponseStatus.UNAUTHORIZED);
        }
    }
}

