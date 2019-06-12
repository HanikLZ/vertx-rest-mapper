package org.mdvsc.vertx.rest;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import org.mdvsc.vertx.collection.GenericMultiMap;
import org.mdvsc.vertx.utils.CollectionUtils;
import org.mdvsc.vertx.utils.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodCache {

    private final Method method;
    private final Annotation[] annotations;
    private final Parameter[] parameters;
    private final Map<Parameter, Annotation[]> parameterMap = new HashMap<>();
    private final Class returnType;
    private int annotatedParameterSize;
    private int mapParameterSize;
    private int fileParameterSize;
    private int defaultValueParameterSize;
    private boolean isHandleEnd;
    private boolean isBlocking;
    private boolean isOrderBlocking;

    MethodCache(Method method) {
        this.method = method;
        this.returnType = method.getReturnType();
        this.parameters = method.getParameters();
        this.annotations = method.getDeclaredAnnotations();
        checkAnnotations();
        checkParameterSize();
    }

    private void checkAnnotations() {
        boolean isBlocking = false, isOrderBlocking = false, isHandleEnd = false;
        for (Annotation a : annotations) {
            if (a instanceof Blocking) {
                Blocking blocking = (Blocking)a;
                isBlocking = true;
                isOrderBlocking = blocking.value();
            } else if (a instanceof HandleEnd) {
                isHandleEnd = true ;
            }
        }
        this.isBlocking = isBlocking;
        this.isOrderBlocking = isOrderBlocking;
        this.isHandleEnd = isHandleEnd;
    }

    private void checkParameterSize() {
        int size = 0;
        int defaultValueSize = 0;
        int mapSize = 0;
        int fileSize = 0;
        for (Parameter parameter : parameters) {
            Annotation[] annotations = parameter.getDeclaredAnnotations();
            parameterMap.put(parameter, annotations);
            for (Annotation annotation : annotations) {
                if (annotation instanceof FieldMap
                            || annotation instanceof QueryMap
                            || annotation instanceof HeaderMap
                            || annotation instanceof FileMap
                            || annotation instanceof PathMap) {
                    mapSize++;
                } else if (annotation instanceof Field) {
                    if (!Constants.isNullValue(((Field)annotation).defaultValue())) defaultValueSize++;
                } else if (annotation instanceof Query) {
                    if (!Constants.isNullValue(((Query)annotation).defaultValue())) defaultValueSize++;
                } else if (annotation instanceof Header) {
                    if (!Constants.isNullValue(((Header)annotation).defaultValue())) defaultValueSize++;
                } else if (annotation instanceof Path) {
                    if (!Constants.isNullValue(((Path)annotation).defaultValue())) defaultValueSize++;
                } else if (annotation instanceof Body) {
                    if (!Constants.isNullValue(((Body)annotation).defaultValue())) defaultValueSize++;
                } else if (annotation instanceof File) {
                    fileSize++;
                } else continue;
                size++;
                break;
            }
        }
        annotatedParameterSize = size;
        mapParameterSize = mapSize;
        fileParameterSize = fileSize;
        defaultValueParameterSize = defaultValueSize;
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
        return CollectionUtils.firstElement(annotations, annotationClazz);
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
     * is method has handled end
     * @return true if yes
     */
    public boolean isHandleEnd() {
        return isHandleEnd;
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
     * annotated parameter size
     * @return parameter size
     */
    public int getAnnotatedParameterSize() {
        return annotatedParameterSize;
    }

    /**
     * annotated but not map parameter size
     * @return
     */
    public int getNonMapAnnotatedParameterSize() {
        return annotatedParameterSize - mapParameterSize;
    }

    /**
     * has annotated but not map parameter
     * @return true if yes
     */
    public boolean hasNonMapAnnotatedParameter() {
        return annotatedParameterSize - mapParameterSize > 0;
    }

    /**
     * parameter size which has default value
     * @return parameter size
     */
    public int getDefaultValueParameterSize() {
        return defaultValueParameterSize;
    }

    /**
     * has parameter with default value
     * @return true if yes
     */
    public boolean hasParameterWithDefaultValue() {
        return defaultValueParameterSize > 0;
    }

    /**
     * map parameter size
     * @return size
     */
    public int getMapParameterSize() {
        return mapParameterSize;
    }

    /**
     * file parameter size
     * @return size
     */
    public int getFileParameterSize() {
        return fileParameterSize;
    }

    /**
     * build method invoke args
     *
     * @param context    routing context
     * @param contextMap context map
     * @return arguments to this method
     * @throws Exception exception if arguments can't be build.
     */
    Object[] buildInvokeArgs(final RoutingContext context, Serializer serializer, ContextProvider provider, Map<Class, Object> contextMap, boolean withDefaultValue) throws Exception {
        HttpServerRequest request = context.request();
        final Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            final Parameter parameter = parameters[i];
            final Class parameterType = parameter.getType();
            Object value = null;
            boolean isContext = false;
            Annotation[] annotations = parameterMap.get(parameter);
            Separator separator = CollectionUtils.firstElement(annotations, Separator.class);
            for (Annotation annotation : annotations) {
                if (annotation instanceof Query) {
                    Query a = (Query) annotation;
                    value = transParams(request.params().getAll(a.value()), withDefaultValue ? a.defaultValue() : null, parameterType, separator, serializer);
                    break;
                } else if (annotation instanceof Header) {
                    Header a = (Header) annotation;
                    value = transParams(request.headers().getAll(a.value()), withDefaultValue ? a.defaultValue() : null, parameterType, separator, serializer);
                    break;
                } else if (annotation instanceof Field) {
                    Field a = (Field) annotation;
                    value = transParams(request.formAttributes().getAll(a.value()), withDefaultValue ? a.defaultValue() : null, parameterType, separator, serializer);
                    break;
                } else if (annotation instanceof Path) {
                    Path a = (Path) annotation;
                    value = transParam(context.pathParam(a.value()), withDefaultValue ? a.defaultValue() : null, parameterType, separator, serializer);
                    break;
                } else if (annotation instanceof File) {
                    Stream<FileUpload> files = context.fileUploads().stream().filter(fileUpload -> ((File) annotation).value().equals(fileUpload.name()));
                    if (parameterType.isArray()) {
                        value = CollectionUtils.toTypedArray(files.collect(Collectors.toList()), parameterType.getComponentType());
                    } else if (List.class.isAssignableFrom(parameterType)) {
                        value = files.collect(Collectors.toList());
                    } else {
                        value = files.findFirst().orElse(null);
                    }
                    break;
                } else if (annotation instanceof FileSet) {
                    value = context.fileUploads();
                    break;
                } else if (annotation instanceof FileMap) {
                    value = context.fileUploads().parallelStream().collect(Collectors.toMap(FileUpload::name, (file) -> file, (k, v) -> v));
                    break;
                } else if (annotation instanceof QueryMap) {
                    value = transMap(request.params(), parameterType);
                    break;
                } else if (annotation instanceof HeaderMap) {
                    value = transMap(request.headers(), parameterType);
                    break;
                } else if (annotation instanceof FieldMap) {
                    value = transMap(request.formAttributes(), parameterType);
                    break;
                } else if (annotation instanceof PathMap) {
                    value = context.pathParams();
                    break;
                } else if (annotation instanceof Body) {
                    if (parameterType == JsonObject.class) {
                        value = context.getBodyAsJson();
                    } else if (parameterType == JsonArray.class) {
                        value = context.getBodyAsJsonArray();
                    } else if (parameterType == Buffer.class) {
                        value = context.getBody();
                    } else if (parameterType == FileUpload.class) {
                        value = context.fileUploads().iterator().next();
                    } else {
                        value = transParam(context.getBodyAsString(), withDefaultValue ? ((Body)annotation).defaultValue() : null, parameterType, separator, serializer);
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

    private static Object transParams(final List<String> params, final String defaultValue, final Class<?> target, final Separator separator, final Serializer serializer) {
        if (params.isEmpty() && Constants.isNullValue(defaultValue)) return null;
        else if (!target.isArray() && !List.class.isAssignableFrom(target)) return transParam(params.isEmpty() ? null : params.get(0), defaultValue, target, separator, serializer);
        if (separator != null) {
            Class element = target.isArray() ? target.getComponentType() : separator.type();
            Object r = params.stream().flatMap(s -> {
                Object value = transListParam(s, element, separator, serializer);
                if (value != null) return ((List) value).stream(); else return Stream.empty();
            }).collect(Collectors.toList());
            List list = (List)r;
            if (list.isEmpty()) list = transListParam(defaultValue, element, separator, serializer);
            return list == null ? null : target.isArray() ? CollectionUtils.toTypedArray(list, target.getComponentType()) : list;
        } else {
            final Class element = target.getComponentType();
            if (element == null) return params.isEmpty() ? Collections.singleton(defaultValue) : params;
            List<Object> list = params.stream()
                    .map(s -> transSimpleParam(s, element, serializer))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (list.isEmpty()) list = Collections.singletonList(transSimpleParam(defaultValue, element, serializer));
            if (list.get(0) == null) return null;
            if (target.isArray()) return CollectionUtils.toTypedArray(list, element);
            return list;
        }
    }

    private static Object transParam(String param, String defaultValue, Class<?> target, Class<?> element, String separator, String start, String end, Serializer serializer) {
        defaultValue = Constants.processNullValue(defaultValue);
        if (target.isArray()) element = target.getComponentType();
        return StringUtils.transObject(param
                , defaultValue
                , target
                , element
                , StringUtils.isNullOrEmpty(separator) ? null : separator
                , StringUtils.isNullOrEmpty(start) ? null : start
                , StringUtils.isNullOrEmpty(end) ? null : end
                , serializer);
    }

    private static Object transParam(String param, String defaultValue, Class<?> target, Separator separator, Serializer serializer) {
        Class type = null;
        String sep = null;
        String start = null;
        String end = null;
        if (separator != null) {
            type = separator.type();
            sep = separator.value();
            start = separator.start();
            end = separator.end();
        }
        return transParam(param, defaultValue, target, type, sep, start, end, serializer);
    }

    private static List transListParam(String param, Class<?> element, Separator separator, Serializer serializer) {
        String sep = null;
        String start = null;
        String end = null;
        if (separator != null) {
            sep = separator.value();
            start = separator.start();
            end = separator.end();
        }
        return (List)transParam(param, null, List.class, element, sep, start, end, serializer);
    }

    private static Object transSimpleParam(String param, Class<?> target, Serializer serializer) {
        return transParam(param, null, target, null, null, null,  null, serializer);
    }

    private static Object transMap(MultiMap content, Class target) {
        return target == MultiMap.class ? new GenericMultiMap(content) : CollectionUtils.toMap(content);
    }

}

