package org.mdvsc.vertx.rest.sample;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.mdvsc.vertx.rest.*;
import org.mdvsc.vertx.utils.StringUtils;

/**
 * @author HanikLZ
 * @since 2017/4/2
 */
public class MyRestServer extends SimpleRestServer {

    public MyRestServer(MyRestServer.Options options) {
        super(options);
        restRouteMapper.addContextClass(RootResource.class, ChildResource.class);
        restRouteMapper.registerContext(MethodInterceptor.class, this::interceptCaller);
        restRouteMapper.registerContext(ResponseFilter.class, this::responseFilter);
        restRouteMapper.registerContext(RequestFilter.class, this::requestFilter);
    }

    private void interceptCaller(MethodCaller caller) {
        if (caller.getMethodCache().firstAnnotation(NeedAuthorize.class) != null) {
            if (!StringUtils.isNullOrBlank(caller.getContext().request().getHeader("token"))) {
                caller.endWithCall();
            } else {
                caller.endWithFail(HttpResponseStatus.UNAUTHORIZED);
            }
        } else caller.endWithCall();
    }

    private void responseFilter(HttpServerRequest request, HttpServerResponse response) {
        String token = request.getHeader("token");
        if (!StringUtils.isNullOrBlank(token)) {
            response.headers().add("authorized", "true");
        }
    }

    private void requestFilter(HttpServerRequest request) {
        String token = request.getHeader("token");
        if (!StringUtils.isNullOrBlank(token)) {
            if ("pass".equals(token)) {
                request.headers().add("userId", "1");
            } else request.headers().remove("token");
        }
    }

}

