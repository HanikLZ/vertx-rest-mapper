package org.mdvsc.vertx.rest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.*;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * @author HanikLZ
 * @since 2017/3/1
 */
public class SimpleRestServer extends AbstractVerticle {

    protected final RestMapper restRouteMapper = new RestMapper();
    protected final Options serverOptions;

    public SimpleRestServer() {
        serverOptions = null;
    }

    public SimpleRestServer(Options serverOptions) {
        this.serverOptions = serverOptions;
    }

    public SimpleRestServer(Options serverOptions, Class... resourceClasses) {
        this.serverOptions = serverOptions;
        restRouteMapper.addContextClass(resourceClasses);
    }

    public SimpleRestServer(Options serverOptions, Object... resources) {
        this.serverOptions = serverOptions;
        restRouteMapper.addContextInstances(resources);
    }

    public SimpleRestServer(Class... resourceClasses) {
        this(null, resourceClasses);
    }

    public SimpleRestServer(Object... resources) {
        this(null, resources);
    }


    @Override
    public void start() throws Exception {
        super.start();
        Router router = Router.router(vertx);
        HttpServer server = onCreateServer(router);
        onInitServerRouter(server, router);
        server.requestHandler(router::accept);
        server.listen(event -> {
            if (event.succeeded()) {
                System.out.println("rest server success listening at port : " + server.actualPort());
            } else if (event.failed()) {
                System.out.println("rest server fail listen at port : " + server.actualPort());
                event.cause().printStackTrace();
            }
        });
    }

    protected HttpServer onCreateServer(Router router) {
        HttpServer server;
        BodyHandler bodyHandler;
        if (serverOptions == null) {
            server = vertx.createHttpServer();
            bodyHandler = BodyHandler.create();
        } else {
            server = vertx.createHttpServer(serverOptions);
            if (serverOptions.uploadPath == null) {
                bodyHandler = BodyHandler.create();
            } else {
                bodyHandler = BodyHandler.create(serverOptions.uploadPath);
            }
            if (serverOptions.bodyLimit > 0) bodyHandler.setBodyLimit(serverOptions.bodyLimit);
            bodyHandler.setDeleteUploadedFilesOnEnd(serverOptions.deleteUploadedFilesOnEnd).setMergeFormAttributes(serverOptions.mergeFormAttributes);
        }
        router.route().handler(bodyHandler);
        router.route().failureHandler(event -> {
            Serializer serializer = restRouteMapper.provideContext(Serializer.class);
            Throwable throwable = event.failure();
            HttpServerResponse response = event.response().setStatusCode(event.statusCode());
            onRouterFailure(response, throwable, serializer);
        });
        return server;
    }

    protected void onInitServerRouter(HttpServer server, Router router) {
        restRouteMapper.addContextInstances(Router.class, router);
        restRouteMapper.addContextInstances(Vertx.class, vertx);
        restRouteMapper.addContextInstances(HttpServer.class, server);
        restRouteMapper.addContextInstances(io.vertx.core.Context.class, context);
        restRouteMapper.applyTo(router);
    }

    protected void onRouterFailure(HttpServerResponse response, Throwable throwable, Serializer serializer) {
        if (response.getStatusCode() <= 0) response.setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        if (throwable != null) {
            response.setStatusMessage(throwable.getMessage()).end(serializer.serialize(throwable.getMessage()));
        } else {
            response.end();
        }
    }

    public static class Options extends HttpServerOptions {

        public String uploadPath = null;
        public int bodyLimit = 0;
        public boolean deleteUploadedFilesOnEnd = true;
        public boolean mergeFormAttributes = false;

        public Options() {
        }

        public Options(JsonObject jsonObject) {
            super(jsonObject);
            uploadPath = jsonObject.getString("uploadPath", uploadPath);
            bodyLimit = jsonObject.getInteger("bodyLimit", bodyLimit);
            deleteUploadedFilesOnEnd = jsonObject.getBoolean("deleteUploadedFilesOnEnd", deleteUploadedFilesOnEnd);
            mergeFormAttributes = jsonObject.getBoolean("mergeFormAttributes", mergeFormAttributes);
        }

    }

}

