package org.mdvsc.vertx.rest;

import java.lang.annotation.*;

/**
 * @author haniklz
 * @since 17-3-1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
public @interface Context { }

