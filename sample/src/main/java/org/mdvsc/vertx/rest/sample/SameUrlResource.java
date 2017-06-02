package org.mdvsc.vertx.rest.sample;

import org.mdvsc.vertx.rest.GET;
import org.mdvsc.vertx.rest.Query;
import org.mdvsc.vertx.rest.QueryMap;
import org.mdvsc.vertx.rest.URL;

import java.util.Map;

/**
 * @author HanikLZ
 * @since 2017/4/2
 */
@URL("/same")
public class SameUrlResource {

    @GET
    @TestFilter
    public Object simpleContent() {
        return "no parameter";
    }

    @GET
    @TestFilter
    public Object simpleContent(@Query("p1") String p1) {
        return String.format("one parameter : [p1 = %s]", p1);
    }

    @GET
    @TestFilter
    public Object simpleContentWithDefault(@Query(value = "p1", defaultValue = "default p1") String p1) {
        return String.format("one parameter and p1 has default value : [p1 = %s]", p1);
    }

    @GET
    @TestFilter
    public Object simpleContent(@Query("p1") String p1, @QueryMap Map<String, String> parameterMap) {
        return String.format("one parameters and a map parameter : [p1 = %s, parameter size = %d]", p1, parameterMap.size());
    }

    @GET
    @TestFilter
    public Object simpleContent(@Query("p1") String p1, @Query("p2") String p2) {
        return String.format("two parameters : [p1 = %s, p2 = %s]", p1, p2);
    }

    @GET
    @TestFilter
    public Object simpleContentWithDefault(@Query("p1") String p1, @Query(value = "p2", defaultValue = "default p2") String p2) {
        return String.format("two parameters and p2 has default value  : [p1 = %s, p2 = %s]", p1, p2);
    }

    @GET
    @TestFilter
    public Object simpleContentWithDefault1(@Query(value = "p1", defaultValue = "default p1") String p1, @Query(value = "p2", defaultValue = "default p2") String p2) {
        return String.format("two parameters and p1 and p2 has default value  : [p1 = %s, p2 = %s]", p1, p2);
    }

    @GET
    @TestFilter
    public Object simpleContent(@Query("p1") String p1, @Query("p2") String p2, @Query("p3") String p3) {
        return String.format("three parameters : [p1 = %s, p2 = %s, p3 = %s]", p1, p2, p3);
    }

    @GET
    @TestFilter
    public Object simpleContentWithDefault(@Query("p1") String p1, @Query("p2") String p2, @Query(value = "p3", defaultValue = "default p3") String p3) {
        return String.format("three parameters and p3 has default value : [p1 = %s, p2 = %s, p3 = %s]", p1, p2, p3);
    }

    @GET
    @TestFilter
    public Object simpleContentWithDefault2(@Query("p1") String p1, @Query(value = "p2", defaultValue = "default p2") String p2, @Query(value = "p3", defaultValue = "default p3") String p3) {
        return String.format("three parameters and p2 and p3 has default value : [p1 = %s, p2 = %s, p3 = %s]", p1, p2, p3);
    }

    @GET
    @TestFilter
    public Object simpleContentWithDefault3(@Query(value = "p1", defaultValue = "default p1") String p1, @Query(value = "p2", defaultValue = "default p2") String p2, @Query(value = "p3", defaultValue = "default p3") String p3) {
        return String.format("three parameters and p1 and p2 and p3 has default value : [p1 = %s, p2 = %s, p3 = %s]", p1, p2, p3);
    }

}
