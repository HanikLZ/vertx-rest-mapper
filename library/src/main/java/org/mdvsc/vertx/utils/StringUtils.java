package org.mdvsc.vertx.utils;

/**
 * @author Home
 * @since 2017/4/3.
 */
public class StringUtils {

    public static boolean isNullOrBlank(String text) {
        return text == null || text.trim().length() == 0;
    }

}
