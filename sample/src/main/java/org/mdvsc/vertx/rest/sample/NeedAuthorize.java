package org.mdvsc.vertx.rest.sample;

import java.lang.annotation.*;

/**
 * @author HanikLZ
 * @since 2017/3/1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface NeedAuthorize {
}

