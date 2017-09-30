package org.mdvsc.vertx.rest;

class Constants {

    final static String DEFAULT_VALUE_NULL = "\u0017\u009CJaVa\u0000vAlUe\u0000NuLl\u0017\u009C";

    static boolean isNullValue(String value) {
        return value == null || DEFAULT_VALUE_NULL.equals(value);
    }

    static String processNullValue(String value) {
        if (isNullValue(value)) return null; else return value;
    }

}
