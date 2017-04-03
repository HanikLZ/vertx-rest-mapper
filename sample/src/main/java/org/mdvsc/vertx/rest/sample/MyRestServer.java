package org.mdvsc.vertx.rest.sample;

import io.vertx.ext.web.RoutingContext;
import org.mdvsc.vertx.ResponseConstants;
import org.mdvsc.vertx.rest.*;

import java.util.stream.Stream;

/**
 * @author HanikLZ
 * @since 2017/4/2
 */
public class MyRestServer extends SimpleRestServer implements MethodInterceptor {

    public MyRestServer() {
        restRouteMapper.addContextClass(SampleResource.class, RootResource.class);
        restRouteMapper.setMethodInterceptor(this);
    }

    @Override
    public void intercept(RoutingContext context, MethodCaller caller) {
        if (Stream.of(caller.getMethodCache().getAnnotations()).anyMatch(annotation -> annotation instanceof TestFilter)) {
            caller.endWithCall(context);
        } else {
            context.fail(ResponseConstants.STATUS_CODE_UNAUTHORIZED);
        }
    }
}

