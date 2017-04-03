package org.mdvsc.vertx.rest;

/**
 * @author HanikLZ
 * @since 2017/3/8
 */

public interface Serializer {

    String mediaEncode();

    String mediaType();

    String serialize(Object object);

    <T> T deserialize(String content, Class<T> clz);

}

