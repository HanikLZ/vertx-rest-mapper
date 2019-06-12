package org.mdvsc.vertx.rest.sample;

import org.mdvsc.vertx.rest.*;

/**
 * @author HanikLZ
 * @since 2017/4/2
 */
@URL(value = "children/:id", ignoreDeploy = true)
@Produces(MediaType.TEXT_PLAIN)
public class ChildResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Object child(@Path("id") int childId) {
        return "child id = " + childId;
    }


}

