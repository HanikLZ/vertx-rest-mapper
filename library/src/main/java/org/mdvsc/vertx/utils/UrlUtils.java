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

    public static String appendUrl(URL base, URL path) {
        boolean hasRegexUrl = base != null && base.regex() || path != null && path.regex();
        String split;
        String url;
        if (hasRegexUrl) split = "\\/"; else split = "/";
        if (base != null && path != null) {
            String baseUrl = trimEndSlash(base);
            String pathUrl = trimStartSlash(path);
            if (hasRegexUrl && base.regex()) baseUrl = replaceRegexChar(baseUrl);
            if (hasRegexUrl && path.regex()) pathUrl = replaceRegexChar(pathUrl);
            url = baseUrl + split + pathUrl;
        } else if (base != null) {
            url = base.value();
        } else if (path != null) {
            url = path.value();
        } else {
            url = null;
        }
        if (url != null && !url.startsWith(split)) {
            url = split + url;
        }
        return url;
    }

}
