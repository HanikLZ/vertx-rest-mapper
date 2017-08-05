package org.mdvsc.vertx.rest;

import io.vertx.core.http.HttpServerRequest;

public interface RequestFilter {
    void filter(HttpServerRequest request);
}

