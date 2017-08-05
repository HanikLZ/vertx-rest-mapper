package org.mdvsc.vertx.rest.sample;

import org.mdvsc.vertx.rest.*;

/**
 * @author HanikLZ
 * @since 2017/4/2
 */
@URL("/")
@Produces(MediaType.TEXT_PLAIN)
public class RootResource {

    @GET
    @NeedAuthorize
    @Produces(MediaType.APPLICATION_JSON)
    public Object simpleContent(@Header(value = "userId", defaultValue = "1") int userId) {
        return "user id = " + userId;
    }

}
