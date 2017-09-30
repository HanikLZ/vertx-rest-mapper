package org.mdvsc.vertx.utils;

import io.vertx.core.MultiMap;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

    public static Object toTypedArray(List list, Class type) {
        int size = list.size();
        Object array = Array.newInstance(type, size);
        for (int i = 0; i < size; i++) {
            Array.set(array, i, list.get(i));
        }
        return array;
    }

    public static boolean isEmptyStringArray(String[] array) {
        boolean empty = true;
        if (array != null) {
            for (String s : array) {
                if (!StringUtils.isNullOrBlank(s)) {
                    empty = false;
                    break;
                }
            }
        }
        return empty;
    }

    public static boolean isEmptyStringCollection(Collection<String> collection) {
        boolean empty = true;
        if (collection != null) {
            for (String s : collection) {
                if (!StringUtils.isNullOrBlank(s)) {
                    empty = false;
                    break;
                }
            }
        }
        return empty;
    }

    public static <E, T extends E> T firstElement(E[] array, Class<T> elementClass) {
        for (E e : array) {
            if (elementClass.isInstance(e)) {
                return (T)e;
            }
        }
        return null;
    }

    public static <E, T extends E, C extends Collection<E>> T firstElement(C collection, Class<T> elementClass) {
        for (E e : collection) {
            if (elementClass.isInstance(e)) {
                return (T)e;
            }
        }
        return null;
    }

}

