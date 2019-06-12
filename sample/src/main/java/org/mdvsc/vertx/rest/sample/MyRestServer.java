package org.mdvsc.vertx.rest.sample;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
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

    @Override
    protected void onInitServerRouter(HttpServer server, Router router) {
        super.onInitServerRouter(server, router);
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

    /*
    @Override
    public void start() throws Exception {
        super.start();

        Router router = Router.router(vertx);

        // Enable multipart form data parsing
        router.route().handler(BodyHandler.create().setUploadsDirectory("uploads"));

        router.route("/").handler(routingContext -> {
            routingContext.response().putHeader("content-type", "text/html").end(
                    "<form action=\"/form\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                            "    <div>\n" +
                            "        <label for=\"name\">Select a file:</label>\n" +
                            "        <input type=\"file\" name=\"file\" />\n" +
                            "    </div>\n" +
                            "    <div class=\"button\">\n" +
                            "        <button type=\"submit\">Send</button>\n" +
                            "    </div>" +
                            "</form>"
            );
        });

        // handle the form
        router.post("/form").handler(ctx -> {
            ctx.response().putHeader("Content-Type", "text/plain");

            ctx.response().setChunked(true);

            for (FileUpload f : ctx.fileUploads()) {
                System.out.println("f");
                ctx.response().write("Filename: " + f.fileName());
                ctx.response().write("\n");
                ctx.response().write("Size: " + f.size());
            }

            ctx.response().end();
        });

        vertx.createHttpServer().requestHandler(router).listen();
    }

     */
}

