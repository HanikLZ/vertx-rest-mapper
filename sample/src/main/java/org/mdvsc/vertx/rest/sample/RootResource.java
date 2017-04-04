package org.mdvsc.vertx.rest.sample;

import org.mdvsc.vertx.rest.GET;
import org.mdvsc.vertx.rest.MediaType;
import org.mdvsc.vertx.rest.Produces;
import org.mdvsc.vertx.rest.URL;

/**
 * @author HanikLZ
 * @since 2017/4/2
 */
@URL("/")
@Produces(MediaType.TEXT_PLAIN)
public class RootResource {

    @GET
    @TestFilter
    @Produces(MediaType.APPLICATION_JSON)
    public Object simpleContent() {
        return "default return";
    }

}
