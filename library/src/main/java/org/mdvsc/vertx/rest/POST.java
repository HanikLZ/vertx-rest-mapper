package org.mdvsc.vertx.rest;

import java.lang.annotation.*;

/**
 * @author HanikLZ
 * @since 2017/3/1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface POST {
    String[] consumes() default "";
    String[] produces() default "";
}

