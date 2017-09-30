package org.mdvsc.vertx.rest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
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
    private String serverOptionConfigKey;
    private HttpServer httpServer;
    private Options serverOptions;

    public SimpleRestServer() {
    }

    public SimpleRestServer(Options serverOptions) {
        this.serverOptions = serverOptions;
    }

    public SimpleRestServer(String optionConfigKey) {
        serverOptionConfigKey = optionConfigKey;
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
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        if (serverOptions == null) {
            JsonObject configObject = config();
            if (configObject != null && serverOptionConfigKey != null && !serverOptionConfigKey.isEmpty()) {
                JsonObject jo = configObject.getJsonObject(serverOptionConfigKey);
                if (jo != null) configObject = jo;
            }
            if (configObject != null) {
                serverOptions = new Options(configObject);
            } else {
                serverOptions = new Options();
            }
        }
    }

    public Options getServerOptions() {
        return serverOptions;
    }

    @Override
    public void start() throws Exception {
        super.start();
        Router router = Router.router(vertx);
        httpServer = onCreateServer(router);
        httpServer.close();
        onInitServerRouter(httpServer, router);
        httpServer.requestHandler(router::accept);
        httpServer.listen(event -> {
            if (event.succeeded()) {
                onServerListening(event.result());
            } else if (event.failed()) {
                onServerListeningFail(event.cause());
            }
        });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (httpServer != null) {
            httpServer.close();
            httpServer = null;
        }
    }

    protected void onServerListening(HttpServer server) {
        LOGGER.log(Level.INFO, "rest server success listening at port : " + server.actualPort());
    }

    protected void onServerListeningFail(Throwable throwable) {
        LOGGER.log(Level.INFO, "rest server start fail : " + throwable.getMessage());
    }

    protected HttpServer onCreateServer(Router router) {
        BodyHandler bodyHandler;
        if (serverOptions.uploadPath == null) {
            bodyHandler = BodyHandler.create();
        } else {
            bodyHandler = BodyHandler.create(serverOptions.uploadPath);
        }
        if (serverOptions.bodyLimit > 0) bodyHandler.setBodyLimit(serverOptions.bodyLimit);
        bodyHandler.setDeleteUploadedFilesOnEnd(serverOptions.deleteUploadedFilesOnEnd).setMergeFormAttributes(serverOptions.mergeFormAttributes);
        router.route().handler(bodyHandler);
        router.route().failureHandler(event -> {
            Serializer serializer = restRouteMapper.provideContext(Serializer.class);
            Throwable throwable = event.failure();
            int errorCode = HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
            if (event.statusCode() > 0) errorCode = event.statusCode();
            onRouterFailure(event.response().setStatusCode(errorCode), throwable, serializer);
        });
        return vertx.createHttpServer(serverOptions);
    }

    protected void onInitServerRouter(HttpServer server, Router router) {
        restRouteMapper.addContextInstances(Router.class, router);
        restRouteMapper.addContextInstances(Vertx.class, vertx);
        restRouteMapper.addContextInstances(HttpServer.class, server);
        restRouteMapper.addContextInstances(io.vertx.core.Context.class, context);
        restRouteMapper.applyTo(router, serverOptions.rootPath);
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
        public String rootPath = null;
        public int bodyLimit = 0;
        public boolean deleteUploadedFilesOnEnd = true;
        public boolean mergeFormAttributes = false;

        public Options() {
        }

        public Options(JsonObject jsonObject) {
            super(jsonObject);
            rootPath = jsonObject.getString("rootPath", rootPath);
            uploadPath = jsonObject.getString("uploadPath", uploadPath);
            bodyLimit = jsonObject.getInteger("bodyLimit", bodyLimit);
            deleteUploadedFilesOnEnd = jsonObject.getBoolean("deleteUploadedFilesOnEnd", deleteUploadedFilesOnEnd);
            mergeFormAttributes = jsonObject.getBoolean("mergeFormAttributes", mergeFormAttributes);
        }

    }

}

