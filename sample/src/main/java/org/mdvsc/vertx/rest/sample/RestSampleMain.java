package org.mdvsc.vertx.rest.sample;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author HanikLZ
 * @since 2017/4/2
 */
public class RestSampleMain {

    public static void main(String... args) {
        MyRestServer.Options config = null;
        try (InputStream is = RestSampleMain.class.getResourceAsStream("/test.json")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\r');
            }
            if (builder.length() > 0) {
                config = Json.decodeValue(builder.toString(), MyRestServer.Options.class);
            }
        } catch (Exception ignored) { }
        Vertx.vertx().deployVerticle(new MyRestServer(config));
    }
}

