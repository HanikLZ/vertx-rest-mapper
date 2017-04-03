package org.mdvsc.vertx.rest;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import org.mdvsc.vertx.utils.UrlUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * restful routing mapper
 * @author HanikLZ
 * @since 2017/3/30
 */
public class RestMapper implements ContextProvider {

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
        int result = o2.getRealParameterSize() - o1.getRealParameterSize();
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
        setSerializer(DEFAULT_SERIALIZER);
        setMethodComparator(DEFAULT_METHOD_COMPARATOR);
    }

    /**
     * apply this mapper to vertx router
     * @param router vertx router
     */
    public void applyTo(final Router router) {
        contextMap.keySet().parallelStream().forEach(clz -> {
            injectContext(contextMap.get(clz));
            applyRouteResource(router, clz);
        });
    }

    /**
     * add context instance for future injecting
     * @param clz class to find this object
     * @param instance instance, maybe null
     * @param <T> class type
     */
    public <T> void addContextInstances(Class<T> clz, T instance) {
        contextMap.put(clz, instance);
    }

    /**
     * add context instance for future injecting
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

    private <T>void setContext(Class<T> clz, T o, T defaultValue) {
        if (o == null) {
            if (defaultValue != null) contextMap.put(clz, defaultValue); else contextMap.remove(clz);
        } else {
            contextMap.put(clz, o);
        }
    }

    @Override
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
        final ContextProvider provider = extraContextProvider;
        T object = provider != null ? provider.provideContext(clz) : null;
        if (object == null) {
            object = (T)contextMap.get(clz);
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

    public void setSerializer(Serializer serializer) {
        setContext(Serializer.class, serializer, DEFAULT_SERIALIZER);
    }

    public void setMethodInterceptor(MethodInterceptor interceptor) {
        setContext(MethodInterceptor.class, interceptor, null);
    }

    public void setExtraContextProvider(ContextProvider provider) {
        extraContextProvider = provider;
    }

    public void setMethodComparator(Comparator<MethodCache> methodComparator) {
        this.methodComparator = methodComparator == null ? DEFAULT_METHOD_COMPARATOR : methodComparator;
    }

    private void applyRouteResource(Router router, Class clz) {
        Annotation[] annotations = clz.getDeclaredAnnotations();
        URL baseUrl = (URL) Arrays.stream(annotations).filter(annotation -> annotation instanceof URL).findFirst().orElse(null);
        for (Method method : clz.getDeclaredMethods()) {
            Annotation[] methodAnnotations = method.getDeclaredAnnotations();
            URL url = (URL) Arrays.stream(methodAnnotations).filter(annotation -> annotation instanceof URL).findFirst().orElse(null);
            boolean hasRegexUrl = false;
            if (baseUrl != null) hasRegexUrl = baseUrl.regex();
            if (url != null) hasRegexUrl |= url.regex();
            String urlStr = UrlUtils.appendUrl(baseUrl, url);
            for (Annotation annotation : methodAnnotations) {
                HttpMethod httpMethod;
                String[] consumes, produces;
                if (annotation instanceof GET) {
                    httpMethod = HttpMethod.GET;
                    consumes = ((GET) annotation).consumes();
                    produces = ((GET) annotation).produces();
                } else if (annotation instanceof POST) {
                    httpMethod = HttpMethod.POST;
                    consumes = ((POST) annotation).consumes();
                    produces = ((POST) annotation).produces();
                } else if (annotation instanceof DELETE) {
                    httpMethod = HttpMethod.DELETE;
                    consumes = ((DELETE) annotation).consumes();
                    produces = ((DELETE) annotation).produces();
                } else if (annotation instanceof PUT) {
                    httpMethod = HttpMethod.PUT;
                    consumes = ((PUT) annotation).consumes();
                    produces = ((PUT) annotation).produces();
                } else if (annotation instanceof HEAD) {
                    httpMethod = HttpMethod.HEAD;
                    consumes = ((HEAD) annotation).consumes();
                    produces = ((HEAD) annotation).produces();
                } else if (annotation instanceof OPTIONS) {
                    httpMethod = HttpMethod.OPTIONS;
                    consumes = ((OPTIONS) annotation).consumes();
                    produces = ((OPTIONS) annotation).produces();
                } else if (annotation instanceof PATCH) {
                    httpMethod = HttpMethod.PATCH;
                    consumes = ((PATCH) annotation).consumes();
                    produces = ((PATCH) annotation).produces();
                } else continue;
                MethodHandler restHandler = methodHandlers.computeIfAbsent(annotation.getClass().getName() + urlStr, k -> new MethodHandler(clz,this))
                        .addHandleMethod(method, methodComparator);
                Route route;
                if (hasRegexUrl) {
                    route = router.routeWithRegex(httpMethod, urlStr);
                } else {
                    route = router.route(httpMethod, urlStr);
                }
                for (String c : consumes) if (!c.isEmpty()) route = route.consumes(c);
                for (String c : produces) if (!c.isEmpty()) route = route.produces(c);
                route.handler(restHandler).failureHandler(restHandler);
            }
        }
    }

}


