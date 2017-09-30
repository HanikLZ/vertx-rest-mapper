package org.mdvsc.vertx.utils;

import org.mdvsc.vertx.rest.Serializer;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Home
 * @since 2017/4/3.
 */
public class StringUtils {

    /**
     * is string null or blank
     * @param text text to test
     * @return true if ye
     */
    public static boolean isNullOrBlank(String text) {
        return text == null || text.trim().length() == 0;
    }

    /**
     * is string null or empty
     * @param text text to test
     * @return true if ye
     */
    public static boolean isNullOrEmpty(String text) {
        return text == null || text.length() == 0;
    }

    /**
     * split text into list by separator
     * @param content content to split
     * @param separator separator string
     * @return list
     */
    public static List<String> split(String content, String separator) {
        List<String> list = new ArrayList<>();
        int separatorPos;
        while ((separatorPos = content.indexOf(separator)) >= 0) {
            list.add(content.substring(0, separatorPos));
            content = content.substring(separatorPos + separator.length());
        }
        if (!content.isEmpty()) list.add(content);
        return list;
    }

    /**
     * split text into array by separator
     * @param content content to split
     * @param separator separator string
     * @return array
     */
    public static String[] splitToArray(String content, String separator) {
        List<String> list = split(content, separator);
        String[] result = new String[list.size()];
        list.toArray(result);
        return result;
    }

    /**
     * parse string to target class object
     * @param content text content
     * @param target target class
     * @param serializer serializer, my be null
     * @param <T> target type
     * @return object
     */
    public static <T> T parseStringValue(String content, Class<T> target, Serializer serializer) {
        Object value = null;
        if (target == String.class) {
            value = content;
        } else {
            content = content.trim();
            if (target == Integer.class || target == int.class) {
                value = Integer.parseInt(content);
            } else if (target == Long.class || target == long.class) {
                value = Long.parseLong(content);
            } else if (target == Float.class || target == float.class) {
                value = Float.parseFloat(content);
            } else if (target == Double.class || target == double.class) {
                value = Double.parseDouble(content);
            } else if (target == Short.class || target == short.class) {
                value = Short.parseShort(content);
            } else if (target == Byte.class || target == byte.class) {
                value = Byte.parseByte(content);
            } else if (target == Boolean.class || target == boolean.class) {
                value = Boolean.parseBoolean(content);
            }
        }
        if (value == null && serializer != null) value = serializer.deserialize(content, target);
        return (T)value;
    }

    /**
     * translate string content to target.
     * @param content content in string
     * @param defaultContent if content is null, this method will trans defaultContent to value
     * @param target target class
     * @param target element class, used when target is Collection or List.
     * @param separator list element separator, such as ","
     * @param start object start string, such as "["
     * @param end object end string, such as "]"
     * @param serializer Serializer
     * @param <T>
     * @return translated object, or throw exception if cannot translate to target class
     */
    public static <T> T transObject(String content, String defaultContent, Class<T> target, Class<?> element, String separator, String start, String end, Serializer serializer) {
        if (element == null) element = String.class;
        if (content == null) if (defaultContent == null) return null; else content = defaultContent;
        content = substringBetween(content, start, end);
        Object value;
        if (target.isArray()) {
            if (separator == null) {
                element = target.getComponentType();
                value = Array.newInstance(element, 1);
                Array.set(value, 0, parseStringValue(content, element, serializer));
            } else {
                value = parseStringArrayValue(content, target, element, separator, start, end, serializer);
            }
        } else if (List.class.isAssignableFrom(target)) {
            if (separator == null) {
                value = Collections.singletonList(parseStringValue(content, element, serializer));
            } else {
                value = parseStringCollectionValue(content, element, separator, start, end, serializer);
            }
        } else {
            value = parseStringValue(content, target, serializer);
        }
        return (T)value;
    }


    /**
     * translate string content to target.
     * @param content content in string
     * @param defaultContent if content is null, this method will trans defaultContent to value
     * @param target target class
     * @param separator list element separator, such as ","
     * @param serializer Serializer
     * @param <T>
     * @return translated object, or throw exception if cannot translate to target class
     */
    public static <T> T transObject(String content, String defaultContent, Class<T> target, String separator, Serializer serializer) {
        return transObject(content, defaultContent, target, String.class, separator, null, null, serializer);
    }

    /**
     * substring between start and end, exclude start and end string.
     * @param content content to substring
     * @param start start string
     * @param end end start
     * @return string
     */
    public static String substringBetween(String content, String start, String end) {
        if (start != null && !start.isEmpty()) {
            int index = content.indexOf(start);
            if (index >= 0) content = content.substring(index + start.length());
        }
        if (end != null && !end.isEmpty()) {
            int index = content.lastIndexOf(end);
            if (index > 0) content = content.substring(0, index);
        }
        return content;
    }

    private static <T> Collection<T> parseStringCollectionValue(String content, Class<T> element, String separator, String start, String end, Serializer serializer) {
        return split(content, separator)
                .stream()
                .map(s -> substringBetween(s, start, end))
                .map(s -> parseStringValue(s, element, serializer))
                .collect(Collectors.toList());
    }

    private static Object parseStringArrayValue(String content, Class target, Class element, String separator, String start, String end, Serializer serializer) {
        Class<?> component = target.getComponentType();
        String trimContent = content.trim();
        List<String> r;
        if (start != null && end != null && trimContent.startsWith(start) && trimContent.endsWith(end)) {
            String[] st = content.split("\\Q" + end + "\\E\\s*\\Q" + separator + "\\E\\s*\\Q" + start + "\\E");
            r = new ArrayList<>(st.length);
            for (int i = 0; i < st.length; i++) {
                String s = st[i];
                if (i > 0) s = start + s;
                if (i < st.length - 1) s += end;
                r.add(s);
            }
        } else if (start != null && end == null && trimContent.startsWith(start)) {
            String[] st = content.split("\\Q" + separator + "\\E\\s*\\Q" + start + "\\E");
            r = new ArrayList<>(st.length);
            for (int i = 0; i < st.length; i++) {
                String s = st[i];
                if (i > 0) s = start + s;
                r.add(s);
            }
        } else if (start == null && end != null && trimContent.endsWith(end)) {
            String[] st = content.split("\\Q" + end + "\\E\\s*\\Q" + separator + "\\E");
            r = new ArrayList<>(st.length);
            for (int i = 0; i < st.length; i++) {
                String s = st[i];
                if (i < st.length - 1) s += end;
                r.add(s);
            }
        } else r = split(content, separator);
        int size = r.size();
        Object value = Array.newInstance(component, size);
        for (int i = 0; i < size; i++) {
            Array.set(value, i, transObject(r.get(i), null, component, element, separator, start, end, serializer));
        }
        return value;
    }

}
