package org.mdvsc.vertx.rest.sample;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.FileUpload;
import org.mdvsc.vertx.rest.*;

import java.util.Map;

/**
 * @author HanikLZ
 * @since 2017/4/2
 */
@URL("sample")
public class SampleResource {

    @GET
    @NeedAuthorize
    public Object simpleContent() {
        return "default return";
    }

    @GET
    @URL("test")
    @NeedAuthorize
    public void simpleContent(@Context HttpServerResponse response, @Context Serializer serializer) {
        response.end(serializer.serialize("no return"));
    }

    @POST
    public Object simplePost() {
        return "post success";
    }

    @POST
    public Object uploadFiles(@FileMap Map<String, FileUpload> files) {
        StringBuilder sb = new StringBuilder();
        files.keySet().stream().map(s -> s + ":" + files.get(s).fileName()).forEach(sb::append);
        return sb;
    }

    @GET
    @URL("child")
    public Object getWithChildPath() {
        return "child path";
    }

    @GET
    @URL("param/:id")
    public Object getWithPathParam(@Path("id") long id) {
        return id;
    }

    @GET
    @URL("custom")
    public void getWithCustomResponse(@Context HttpServerResponse response) {
        response.end("custom response!");
    }

}
