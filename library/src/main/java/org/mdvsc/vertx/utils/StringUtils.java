package org.mdvsc.vertx.utils;

import org.mdvsc.vertx.rest.Serializer;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Home
 * @since 2017/4/3.
 */
public class StringUtils {

    public static boolean isNullOrBlank(String text) {
        return text == null || text.trim().length() == 0;
    }

    /**
     * translate string content to target.
     * @param content content in string
     * @param defaultContent if content is null, this method will trans defaultContent to value
     * @param target target class
     * @param serializer Serializer
     * @return translated object, or throw exception if cannot translate to target class
     */
    public static Object transObject(String content, String defaultContent, Class target, Serializer serializer) {
        if (content == null) content = defaultContent;
        Object value = content;
        if (target != String.class) {
            if (target == Integer.class) {
                value = Integer.parseInt(content);
            } else if (target == Long.class) {
                value = Long.parseLong(content);
            } else if (target == Float.class) {
                value = Float.parseFloat(content);
            } else if (target == Double.class) {
                value = Double.parseDouble(content);
            } else if (target == Short.class) {
                value = Short.parseShort(content);
            } else if (target == Byte.class) {
                value = Byte.parseByte(content);
            } else if (target == Boolean.class) {
                value = Boolean.parseBoolean(content);
            } else if (target == String[].class) {
                value = content.split(Pattern.quote(","));
            } else if (target == List.class) {
                value = Arrays.asList(content.split(Pattern.quote(",")));
            } else if (target == int[].class) {
                value = Arrays.stream(content.split(Pattern.quote(","))).mapToInt(Integer::parseInt).toArray();
            } else if (target == long[].class) {
                value = Arrays.stream(content.split(Pattern.quote(","))).mapToLong(Long::parseLong).toArray();
            } else if (target == double[].class) {
                value = Arrays.stream(content.split(Pattern.quote(","))).mapToDouble(Double::parseDouble).toArray();
            } else if (target == short[].class) {
                String[] r = content.split(Pattern.quote(","));
                short[] sr = new short[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Short.parseShort(r[i]);
                }
                value = sr;
            } else if (target == float[].class) {
                String[] r = content.split(Pattern.quote(","));
                float[] sr = new float[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Float.parseFloat(r[i]);
                }
                value = sr;
            } else if (target == byte[].class) {
                String[] r = content.split(Pattern.quote(","));
                byte[] sr = new byte[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Byte.parseByte(r[i]);
                }
                value = sr;
            } else if (target == boolean[].class) {
                String[] r = content.split(Pattern.quote(","));
                boolean[] sr = new boolean[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Boolean.parseBoolean(r[i]);
                }
                value = sr;
            } else if (target == Integer[].class) {
                String[] r = content.split(Pattern.quote(","));
                Integer[] sr = new Integer[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Integer.parseInt(r[i]);
                }
                value = sr;
            } else if (target == Long[].class) {
                String[] r = content.split(Pattern.quote(","));
                Long[] sr = new Long[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Long.parseLong(r[i]);
                }
                value = sr;
            } else if (target == Double[].class) {
                String[] r = content.split(Pattern.quote(","));
                Double[] sr = new Double[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Double.parseDouble(r[i]);
                }
                value = sr;
            } else if (target == Short[].class) {
                String[] r = content.split(Pattern.quote(","));
                Short[] sr = new Short[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Short.parseShort(r[i]);
                }
                value = sr;
            } else if (target == Float[].class) {
                String[] r = content.split(Pattern.quote(","));
                Float[] sr = new Float[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Float.parseFloat(r[i]);
                }
                value = sr;
            } else if (target == Byte[].class) {
                String[] r = content.split(Pattern.quote(","));
                Byte[] sr = new Byte[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Byte.parseByte(r[i]);
                }
                value = sr;
            } else if (target == Boolean[].class) {
                String[] r = content.split(Pattern.quote(","));
                Boolean[] sr = new Boolean[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Boolean.parseBoolean(r[i]);
                }
                value = sr;
            } else {
                value = serializer.deserialize(content, target);
            }
        }
        return value;
    }

}
