package org.mdvsc.vertx.utils;

import org.mdvsc.vertx.rest.URL;

/**
 * Utils for url process
 * @author HanikLZ
 * @since 2017/3/1
 */
public class UrlUtils {

    private final static String REG_CHARS = "*.?+$^[](){}|\\/";

    public static String replaceRegexChar(String url) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < url.length(); i++) {
            char ch = url.charAt(i);
            if (REG_CHARS.indexOf(ch) >= 0) {
                sb.append('\\').append(ch);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static String trimStartSlash(URL url) {
        String split;
        if (url.regex()) split = "\\/"; else split = "/";
        String path = url.value();
        if (path.startsWith(split)) {
            path = path.substring(split.length());
        }
        return path;
    }

    public static String trimEndSlash(URL url) {
        String split;
        if (url.regex()) split = "\\/"; else split = "/";
        String path = url.value();
        if (path.endsWith(split)) {
            path = path.substring(0, path.length() - split.length());
        }
        return path;
    }

    public static String appendUrl(String root, URL... paths) {
        String split = "/";
        String joinedUrl = root == null ? split : root.endsWith(split) ? root.substring(0, root.length() - split.length()) : root;
        boolean hasRegexUrl = false;
        for (URL path : paths) {
            if (path == null) continue;
            String pathUrl = trimStartSlash(path);
            if (!hasRegexUrl && path.regex()) {
                hasRegexUrl = true;
                joinedUrl = replaceRegexChar(joinedUrl);
                split = "\\/";
            }
            joinedUrl = joinedUrl + split + (path.regex() ? replaceRegexChar(pathUrl) : pathUrl);
        }
        if (!joinedUrl.startsWith(split)) {
            joinedUrl = split + joinedUrl;
        }
        return joinedUrl;
    }

}
