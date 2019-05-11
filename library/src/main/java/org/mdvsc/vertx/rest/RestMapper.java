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
import java.util.stream.Stream;

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

    private void applyRouteResource(Router router, String baseUrl, Class clz) {
        UrlHolder baseUrlHolder = UrlHolder.fromClass(clz, baseUrl);
        if (baseUrlHolder == null || !baseUrlHolder.url.auto()) return;
        applyRouteResource(router, clz, baseUrlHolder.getUrl(), baseUrlHolder.hasRegexUrl(), baseUrlHolder.produces, baseUrlHolder.consumes);
        applyChildRouteResource(router, baseUrlHolder, false, null, null);
    }

    private void applyChildRouteResource(Router router, UrlHolder urlHolder, boolean hasRegexUrl, Produces defaultProduces, Consumes defaultConsumes) {
        Class child = urlHolder.getUrlChild();
        if (child != null) {
            UrlHolder childUrlHolder = UrlHolder.fromClass(child, urlHolder.getUrl());
            defaultProduces = childUrlHolder.produces == null ? urlHolder.produces == null ? defaultProduces : urlHolder.produces : childUrlHolder.produces;
            defaultConsumes = childUrlHolder.consumes == null ? urlHolder.consumes == null ? defaultConsumes : urlHolder.consumes : childUrlHolder.consumes;
            applyRouteResource(router, child, childUrlHolder.getUrl(), childUrlHolder.hasRegexUrl() || urlHolder.hasRegexUrl() || hasRegexUrl, defaultProduces, defaultConsumes);
        }
    }

    private void applyRouteResource(Router router, Class clz, String baseUrl, boolean hasRegexUrl, Produces produces, Consumes consumes) {
        for (Method method : clz.getDeclaredMethods()) {
            Annotation[] methodAnnotations = method.getDeclaredAnnotations();
            UrlHolder urlHolder = UrlHolder.fromAnnotations(methodAnnotations, baseUrl, true);
            hasRegexUrl |= urlHolder.hasRegexUrl();
            if (urlHolder.produces != null) produces = urlHolder.produces;
            if (urlHolder.consumes != null) consumes = urlHolder.consumes;
            String urlStr = urlHolder.getUrl();
            for (Annotation annotation : methodAnnotations) {
                HttpMethod httpMethod = ANNOTATION_MAP.get(annotation.annotationType());
                if (httpMethod != null) {
                    final boolean autoDeploy = urlHolder.getUrlChild() != null && urlHolder.url.auto();
                    final boolean applyRegexUrl = hasRegexUrl || autoDeploy;
                    final String applyUrlStr = autoDeploy ? urlStr + "/.*" : urlStr;
                    final char splitChar = ':';
                    StringBuilder builder = new StringBuilder()
                            .append(applyRegexUrl)
                            .append(splitChar)
                            .append(annotation)
                            .append(splitChar)
                            .append(applyUrlStr)
                            .append(splitChar);
                    if (consumes != null) {
                        for (String c : consumes.value()) {
                            builder.append(c).append('|');
                        }
                    }
                    builder.append(splitChar);
                    if (produces != null) {
                        for (String c : produces.value()) {
                            builder.append(c).append('|');
                        }
                    }
                    final String methodKey = builder.toString();
                    MethodHandler restHandler = methodHandlers.get(methodKey);
                    if (restHandler == null) {
                        restHandler = new MethodHandler(clz, this);
                        methodHandlers.put(methodKey, restHandler);
                        Route route;
                        if (applyRegexUrl) route = router.routeWithRegex(httpMethod, applyUrlStr);
                        else route = router.route(httpMethod, applyUrlStr);
                        if (consumes != null) {
                            for (String c : consumes.value()) {
                                if (!StringUtils.isNullOrBlank(c)) route = route.consumes(c.trim());
                            }
                        }
                        if (produces != null) {
                            for (String p : produces.value()) {
                                if (!StringUtils.isNullOrBlank(p)) route = route.produces(p.trim());
                            }
                        }
                        route.handler(restHandler).failureHandler(restHandler);
                    }
                    restHandler.addHandleMethod(method, methodComparator);
                }
                applyChildRouteResource(router, urlHolder, hasRegexUrl, produces, consumes);
            }
        }
    }

    private static class UrlHolder {

        final URL url;
        final Produces produces;
        final Consumes consumes;

        private String baseUrl;

        static UrlHolder fromAnnotations(Annotation[] annotations, String baseUrl, boolean enableNullUrl) {
            URL url = null;
            Produces produces = null;
            Consumes consumes = null;
            for (Annotation annotation : annotations) {
                if (annotation instanceof URL) {
                    url = (URL) annotation;
                } else if (annotation instanceof Produces) {
                    produces = (Produces) annotation;
                } else if (annotation instanceof Consumes) {
                    consumes = (Consumes) annotation;
                }
            }
            UrlHolder holder = null;
            if (url != null || enableNullUrl) {
                holder = new UrlHolder(url, produces, consumes);
                holder.setBaseUrl(baseUrl);
            }
            return holder;
        }

        static UrlHolder fromClass(Class clz, String baseUrl) {
            return fromAnnotations(clz.getDeclaredAnnotations(), baseUrl, false);
        }

        private UrlHolder(URL url, Produces produces, Consumes consumes) {
            this.url = url;
            this.produces = produces;
            this.consumes = consumes;
        }

        void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        boolean hasRegexUrl() {
            return url != null && url.regex();
        }

        Class getUrlChild() {
            Class child = url == null ? null : url.child();
            if (child == URL.class) {
                child = null;
            }
            return child;
        }

        String getUrl() {
            if (url == null) return baseUrl;
            String actualUrl = baseUrl == null ? url.value() : UrlUtils.appendUrl(baseUrl, url);
            return actualUrl == null ? "/" : !actualUrl.startsWith("/") ? "/" + actualUrl : actualUrl;
        }

    }

}


