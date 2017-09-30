package org.mdvsc.vertx.rest;

import java.lang.annotation.*;

/**
 * @author HanikLZ
 * @since 2017/3/1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface Separator {
    String value() default "";
    String start() default "";
    String end() default "";
    Class type() default String.class;
}

