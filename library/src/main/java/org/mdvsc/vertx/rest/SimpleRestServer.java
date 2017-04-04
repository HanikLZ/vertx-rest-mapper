package org.mdvsc.vertx.rest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.*;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.mdvsc.vertx.utils.StringUtils;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author HanikLZ
 * @since 2017/3/1
 */
public class SimpleRestServer extends AbstractVerticle {

    private static final Logger LOGGER = Logger.getLogger("simpleRestServer");

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
                LOGGER.log(Level.INFO, "rest server success listening at port : " + server.actualPort());
            } else if (event.failed()) {
                LOGGER.log(Level.INFO, "rest server fail listen at port : " + server.actualPort());
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
            int errorCode = HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
            if (event.statusCode() > 0) errorCode = event.statusCode();
            onRouterFailure(event.response().setStatusCode(errorCode), throwable, serializer);
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
        String content = null;
        if (throwable != null) {
            String message = throwable.getMessage();
            message = StringUtils.isNullOrBlank(message) ? throwable.getClass().getSimpleName() : message;
            response.setStatusMessage(message.replace('\r', ' ').replace('\n', ' '));
            content = buildErrorMessage(throwable, serializer);
        }
        if (StringUtils.isNullOrBlank(content)) response.end(); else response.end(content);
    }

    protected String buildErrorMessage(Throwable e, Serializer serializer) {
        return serializer.serialize(Stream.of(e.getStackTrace()).map(element -> new StringBuilder()
                .append(element.getLineNumber())
                .append(':')
                .append(element.getFileName())
                .append('/')
                .append(element.getClassName())
                .append('.')
                .append(element.getMethodName())).collect(Collectors.toList()));
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

