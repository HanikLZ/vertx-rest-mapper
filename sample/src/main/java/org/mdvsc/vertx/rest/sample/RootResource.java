package org.mdvsc.vertx.rest.sample;

import org.mdvsc.vertx.rest.GET;
import org.mdvsc.vertx.rest.URL;

/**
 * @author HanikLZ
 * @since 2017/4/2
 */
@URL("/")
public class RootResource {

    @GET
    @TestFilter
    public Object simpleContent() {
        return "default return";
    }

}
