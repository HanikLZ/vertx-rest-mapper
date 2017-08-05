package org.mdvsc.vertx.rest;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import org.mdvsc.vertx.utils.CollectionUtils;
import org.mdvsc.vertx.utils.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodCache {

    private final Method method;
    private final Annotation[] annotations;
    private final Parameter[] parameters;
    private final Map<Parameter, Annotation[]> parameterMap = new HashMap<>();
    private final int realParameterSize;
    private final int mapParameterSize;
    private final Class returnType;
    private final boolean isBlocking;
    private final boolean isOrderBlocking;

    MethodCache(Method method) {
        this.method = method;
        this.returnType = method.getReturnType();
        this.parameters = method.getParameters();
        this.annotations = method.getDeclaredAnnotations();
        Blocking blocking = firstAnnotation(Blocking.class);
        if (blocking != null) {
            this.isBlocking = true;
            this.isOrderBlocking = blocking.value();
        } else {
            this.isBlocking = false;
            this.isOrderBlocking = false;
        }

        int size = 0;
        int mapSize = 0;
        for (Parameter parameter : parameters) {
            Annotation[] annotations = parameter.getDeclaredAnnotations();
            parameterMap.put(parameter, annotations);
            for (Annotation annotation : annotations) {
                if (annotation instanceof Field
                        || annotation instanceof Query
                        || annotation instanceof Header
                        || annotation instanceof File
                        || annotation instanceof Path) {
                    size++;
                    break;
                } else if (annotation instanceof FieldMap
                            || annotation instanceof QueryMap
                            || annotation instanceof HeaderMap
                            || annotation instanceof FileMap
                            || annotation instanceof PathMap) {
                    mapSize++;
                    break;
                }
            }
        }
        realParameterSize = size;
        mapParameterSize = mapSize;
    }

    /**
     * method has return?
     * @return true if has return
     */
    public boolean hasReturn() {
        return method.getReturnType() != Void.TYPE;
    }

    /**
     * get actual method instance
     * @return method instance
     */
    public Method getMethod() {
        return method;
    }

    /**
     * get return type
     * @return method return type
     */
    public Class getReturnType() {
        return returnType;
    }

    /**
     * get method parameters
     * @return parameters
     */
    public Parameter[] getParameters() {
        return parameters;
    }

    /**
     * get method annotations
     * @return annotation array
     */
    public Annotation[] getAnnotations() {
        return annotations;
    }

    /**
     * find first matched annotation
     * @param annotationClazz annotation class to find.
     * @param <T> class type
     * @return annotation instance or null if not find.
     */
    public<T extends Annotation> T firstAnnotation(Class<T> annotationClazz) {
        for (Annotation a : annotations) {
            if (annotationClazz.isInstance(a)) {
                return (T)a;
            }
        }
        return null;
    }

    /**
     * is blocking method.
     * @return true if yes
     */
    public boolean isBlocking() {
        return isBlocking;
    }

    /**
     * is order blocking method.
     * @return true if yes
     */
    public boolean isOrderBlocking() {
        return isOrderBlocking;
    }

    /**
     * get parameter annotations
     * @param parameter parameter
     * @return annotations
     */
    public Annotation[] getParamterAnnotations(Parameter parameter) {
        return parameterMap.get(parameter);
    }

    /**
     * parameter size without annotation Context and mapped parameters
     * @return parameter size without annotation Context
     */
    public int getRealParameterSize() {
        return realParameterSize;
    }

    /**
     * map parameter size
     * @return size
     */
    public int getMapParameterSize() {
        return mapParameterSize;
    }

    /**
     * build method invoke args
     *
     * @param context    routing context
     * @param contextMap context map
     * @return arguments to this method
     * @throws Exception exception if arguments can't be build.
     */
    Object[] buildInvokeArgs(final RoutingContext context, Serializer serializer, ContextProvider provider, Map<Class, Object> contextMap) throws Exception {
        HttpServerRequest request = context.request();
        contextMap.put(HttpServerRequest.class, request);
        final Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = null;
            boolean isContext = false;
            for (Annotation annotation : parameterMap.get(parameter)) {
                if (annotation instanceof Query) {
                    Query a = (Query) annotation;
                    value = transFromAnnotation(request.getParam(a.value()), a.defaultValue(), parameter.getType(), serializer);
                    break;
                } else if (annotation instanceof Header) {
                    Header a = (Header) annotation;
                    value = transFromAnnotation(request.getHeader(a.value()), a.defaultValue(), parameter.getType(), serializer);
                    break;
                } else if (annotation instanceof Field) {
                    Field a = (Field) annotation;
                    value = transFromAnnotation(request.getFormAttribute(a.value()), a.defaultValue(), parameter.getType(), serializer);
                    break;
                } else if (annotation instanceof Path) {
                    Path a = (Path) annotation;
                    value = transFromAnnotation(context.pathParam(a.value()), a.defaultValue(), parameter.getType(), serializer);
                    break;
                } else if (annotation instanceof File) {
                    value = context.fileUploads().parallelStream().filter(fileUpload -> ((File) annotation).value().equals(fileUpload.name())).findFirst();
                    break;
                } else if (annotation instanceof FileMap) {
                    value = context.fileUploads().parallelStream().collect(Collectors.toMap(FileUpload::name, (file) -> file, (k, v) -> v));
                } else if (annotation instanceof QueryMap) {
                    value = CollectionUtils.toMap(request.params());
                    break;
                } else if (annotation instanceof HeaderMap) {
                    value = CollectionUtils.toMap(request.headers());
                    break;
                } else if (annotation instanceof FieldMap) {
                    value = CollectionUtils.toMap(request.formAttributes());
                    break;
                } else if (annotation instanceof PathMap) {
                    value = context.pathParams();
                    break;
                } else if (annotation instanceof Body) {
                    Class type = parameter.getType();
                    if (type == JsonObject.class) {
                        value = context.getBodyAsJson();
                    } else if (type == JsonArray.class) {
                        value = context.getBodyAsJsonArray();
                    } else if (type == Buffer.class) {
                        value = context.getBody();
                    } else if (type == FileUpload.class) {
                        value = context.fileUploads().iterator().next();
                    } else {
                        value = transFromAnnotation(context.getBodyAsString(), ((Body)annotation).defaultValue(), type, serializer);
                    }
                    break;
                } else if (annotation instanceof Context) {
                    value = provideContext(parameter.getType(), provider, contextMap);
                    isContext = true;
                    break;
                }
            }
            if (value == null && !isContext) throw new IllegalAccessException();
            args[i] = value;
        }
        return args;
    }

    private Object provideContext(final Class<?> clz, final ContextProvider provider, final Map<Class, Object> map) {
        Object object = map == null ? null : map.get(clz);
        if (object == null) {
            object = provider.provideContext(clz);
        }
        return object;
    }

    private static Object transFromAnnotation(String content, String defaultContent, Class target, Serializer serializer) {
        if (defaultContent != null && defaultContent.isEmpty()) defaultContent = null;
        return StringUtils.transObject(content, defaultContent, target, serializer);
    }

}
