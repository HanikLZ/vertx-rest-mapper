package org.mdvsc.vertx.utils;

import io.vertx.core.MultiMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Utils for collection process
 * @author HanikLZ
 * @since 2017/3/19
 */
public class CollectionUtils {

    /**
     * MultiMap to Map
     * @param map input map
     * @return output map
     */
    public static Map<String, String> toMap(MultiMap map) {
        Map<String, String> value = new HashMap<>();
        map.forEach(entry -> value.put(entry.getKey(), entry.getValue()));
        return value;
    }

}

