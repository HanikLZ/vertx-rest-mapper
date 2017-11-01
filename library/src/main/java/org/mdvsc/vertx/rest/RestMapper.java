package org.mdvsc.vertx.rest;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import org.mdvsc.vertx.utils.StringUtils;
import org.mdvsc.vertx.utils.UrlUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * restful routing mapper
 *
 * @author HanikLZ
 * @since 2017/3/30
 */
public class RestMapper implements ContextProvider {

    private static final Map<Class<? extends Annotation>, HttpMethod> ANNOTATION_MAP = new HashMap<>();

    static {
        ANNOTATION_MAP.put(GET.class, HttpMethod.GET);
        ANNOTATION_MAP.put(PUT.class, HttpMethod.PUT);
        ANNOTATION_MAP.put(POST.class, HttpMethod.POST);
        ANNOTATION_MAP.put(HEAD.class, HttpMethod.HEAD);
        ANNOTATION_MAP.put(PATCH.class, HttpMethod.PATCH);
        ANNOTATION_MAP.put(DELETE.class, HttpMethod.DELETE);
        ANNOTATION_MAP.put(OPTIONS.class, HttpMethod.OPTIONS);
    }

    private static final Serializer DEFAULT_SERIALIZER = new Serializer() {
        @Override
        public String mediaEncode() {
            return "utf-8";
        }

        @Override
        public String mediaType() {
            return MediaType.APPLICATION_JSON;
        }

        @Override
        public String serialize(Object object) {
            return Json.encodePrettily(object);
        }

        @Override
        public <T> T deserialize(String content, Class<T> clz) {
            return Json.decodeValue(content, clz);
        }
    };

    private static final Comparator<MethodCache> DEFAULT_METHOD_COMPARATOR = (o1, o2) -> {

        // more size of parameters without map parameters match first
        int result = o2.getNonMapAnnotatedParameterSize() - o1.getNonMapAnnotatedParameterSize();

        // less size of parameters with default value match first
        if (result == 0) result = o1.getDefaultValueParameterSize() - o2.getDefaultValueParameterSize();

        // more size of mapped parameter size match first
        if (result == 0) result = o2.getMapParameterSize() - o1.getMapParameterSize();

        return result;

    };

    private final Map<Class, Object> contextMap = new HashMap<>();
    private final Map<String, MethodHandler> methodHandlers = new HashMap<>();
    private ContextProvider extraContextProvider = null;
    private Comparator<MethodCache> methodComparator = null;

    /**
     * default constructor
     */
    public RestMapper() {
        registerContext(Serializer.class, DEFAULT_SERIALIZER);
        setMethodComparator(DEFAULT_METHOD_COMPARATOR);
    }

    /**
     * apply this mapper to vertx router
     *
     * @param router vertx router
     */
    public void applyTo(final Router router, String root) {
        contextMap.keySet().forEach(clz -> {
            injectContext(contextMap.get(clz));
            applyRouteResource(router, root, clz);
        });
    }

    /**
     * add context instance for future injecting
     *
     * @param clz      class to find this object
     * @param instance instance, maybe null
     * @param <T>      class type
     */
    public <T> void addContextInstances(Class<T> clz, T instance) {
        contextMap.put(clz, instance);
    }

    /**
     * add context instance for future injecting
     *
     * @param contexts context objects
     */
    public void addContextInstances(Object... contexts) {
        for (Object o : contexts) {
            addContext(o.getClass(), o);
        }
    }

    public void addContextClass(Class... classes) {
        for (Class clz : classes) {
            addContext(clz, null);
        }
    }

    private void addContext(Class clz, Object o) {
        contextMap.put(clz, o);
        Class[] interfaces = clz.getInterfaces();
        if (interfaces != null) {
            for (Class c : interfaces) {
                contextMap.put(c, o);
            }
        }
    }

    public <T> void registerContext(Class<T> clz, T o) {
        if (o == null) throw new NullPointerException("register null context.");
        contextMap.put(clz, o);
    }

    public <T> T unregisterContext(Class<T> clz) {
        return (T)contextMap.remove(clz);
    }

    public void injectContext(Object object) {
        if (object != null) {
            for (java.lang.reflect.Field field : object.getClass().getFields()) {
                Context context = field.getAnnotation(Context.class);
                if (context == null) continue;
                Object value = provideContext(field.getType());
                if (value == null) continue;
                try {
                    field.set(object, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public <T> T provideContext(Class<T> clz) {
        T object = (T) contextMap.get(clz);
        if (object == null) {
            final ContextProvider provider = extraContextProvider;
            object = provider != null ? provider.provideContext(clz) : null;
        }
        if (object == null && contextMap.containsKey(clz)) {
            try {
                object = clz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new UnsupportedOperationException(String.format("Class %s has no default constructor.", clz.getName()));
            }
            contextMap.put(clz, object);
            injectContext(object);
        }
        return object;
    }

    public void setExtraContextProvider(ContextProvider provider) {
        extraContextProvider = provider;
    }

    public void setMethodComparator(Comparator<MethodCache> methodComparator) {
        this.methodComparator = methodComparator;
    }

    private void applyRouteResource(Router router, String root, Class clz) {
        Annotation[] annotations = clz.getDeclaredAnnotations();
        UrlHolder baseUrlHolder = new UrlHolder(annotations);
        for (Method method : clz.getDeclaredMethods()) {
            Annotation[] methodAnnotations = method.getDeclaredAnnotations();
            Produces produces = baseUrlHolder.produces;
            Consumes consumes = baseUrlHolder.consumes;
            UrlHolder urlHolder = new UrlHolder(methodAnnotations);
            boolean hasRegexUrl = false;
            if (baseUrlHolder.url != null) hasRegexUrl = baseUrlHolder.url.regex();
            if (urlHolder.url != null) hasRegexUrl |= urlHolder.url.regex();
            if (urlHolder.produces != null) produces = urlHolder.produces;
            if (urlHolder.consumes != null) consumes = urlHolder.consumes;
            String urlStr = UrlUtils.appendUrl(root, baseUrlHolder.url, urlHolder.url);
            for (Annotation annotation : methodAnnotations) {
                HttpMethod httpMethod = ANNOTATION_MAP.get(annotation.annotationType());
                if (httpMethod == null) continue;
                MethodHandler restHandler = methodHandlers
                        .computeIfAbsent(annotation.getClass().getName() + urlStr, k -> new MethodHandler(clz, this))
                        .addHandleMethod(method, methodComparator);
                Route route;
                if (hasRegexUrl) {
                    route = router.routeWithRegex(httpMethod, urlStr);
                } else {
                    route = router.route(httpMethod, urlStr);
                }
                if (consumes != null) for (String c : consumes.value()) if (!StringUtils.isNullOrBlank(c)) route = route.consumes(c.trim());
                if (produces != null) for (String p : produces.value()) if (!StringUtils.isNullOrBlank(p)) route = route.produces(p.trim());
                route.handler(restHandler).failureHandler(restHandler);
            }
        }
    }

    private class UrlHolder {

        URL url;
        Produces produces;
        Consumes consumes;

        UrlHolder(Annotation[] annotations) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof URL) {
                    url = (URL) annotation;
                } else if (annotation instanceof Produces) {
                    produces = (Produces) annotation;
                } else if (annotation instanceof Consumes) {
                    consumes = (Consumes) annotation;
                }
            }
        }
    }

}


