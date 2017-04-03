package org.mdvsc.vertx.rest;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import org.mdvsc.vertx.utils.CollectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MethodCache {

    private final Method method;
    private final Annotation[] annotations;
    private final Parameter[] parameters;
    private final Map<Parameter, Annotation[]> parameterMap = new HashMap<>();
    private final int realParameterSize;
    private final int mapParameterSize;
    private final Class returnType;

    MethodCache(Method method) {
        this.method = method;
        this.returnType = method.getReturnType();
        this.parameters = method.getParameters();
        this.annotations = method.getDeclaredAnnotations();
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
     * @return anootation array
     */
    public Annotation[] getAnnotations() {
        return annotations;
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
                    value = transObject(request.getParam(((Query) annotation).value()), parameter.getType(), serializer);
                    break;
                } else if (annotation instanceof Header) {
                    value = transObject(request.getHeader(((Header) annotation).value()), parameter.getType(), serializer);
                    break;
                } else if (annotation instanceof Field) {
                    value = transObject(request.getFormAttribute(((Field) annotation).value()), parameter.getType(), serializer);
                    break;
                } else if (annotation instanceof Path) {
                    value = transObject(context.pathParam(((Path) annotation).value()), parameter.getType(), serializer);
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
                        value = transObject(context.getBodyAsString(), type, serializer);
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


    private Object provideContext(final Class clz, final ContextProvider provider, final Map<Class, Object> map) {
        Object object = map == null ? null : map.get(clz);
        if (object == null) {
            object = provider.provideContext(clz);
        }
        return object;
    }


    /**
     * translate string content to target.
     * @param content content in string
     * @param target target class
     * @param serializer Serializer
     * @return translated object, or throw exception if cannot translate to target class
     */
    public static Object transObject(String content, Class target, Serializer serializer) {
        Object value = content;
        if (target != String.class) {
            if (target == Integer.class) {
                value = Integer.parseInt(content);
            } else if (target == Long.class) {
                value = Long.parseLong(content);
            } else if (target == Float.class) {
                value = Float.parseFloat(content);
            } else if (target == Double.class) {
                value = Double.parseDouble(content);
            } else if (target == Short.class) {
                value = Short.parseShort(content);
            } else if (target == Byte.class) {
                value = Byte.parseByte(content);
            } else if (target == Boolean.class) {
                value = Boolean.parseBoolean(content);
            } else if (target == String[].class) {
                value = content.split(Pattern.quote(","));
            } else if (target == List.class) {
                value = Arrays.asList(content.split(Pattern.quote(",")));
            } else if (target == int[].class) {
                value = Arrays.stream(content.split(Pattern.quote(","))).mapToInt(Integer::parseInt).toArray();
            } else if (target == long[].class) {
                value = Arrays.stream(content.split(Pattern.quote(","))).mapToLong(Long::parseLong).toArray();
            } else if (target == double[].class) {
                value = Arrays.stream(content.split(Pattern.quote(","))).mapToDouble(Double::parseDouble).toArray();
            } else if (target == short[].class) {
                String[] r = content.split(Pattern.quote(","));
                short[] sr = new short[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Short.parseShort(r[i]);
                }
                value = sr;
            } else if (target == float[].class) {
                String[] r = content.split(Pattern.quote(","));
                float[] sr = new float[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Float.parseFloat(r[i]);
                }
                value = sr;
            } else if (target == byte[].class) {
                String[] r = content.split(Pattern.quote(","));
                byte[] sr = new byte[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Byte.parseByte(r[i]);
                }
                value = sr;
            } else if (target == boolean[].class) {
                String[] r = content.split(Pattern.quote(","));
                boolean[] sr = new boolean[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Boolean.parseBoolean(r[i]);
                }
                value = sr;
            } else if (target == Integer[].class) {
                String[] r = content.split(Pattern.quote(","));
                Integer[] sr = new Integer[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Integer.parseInt(r[i]);
                }
                value = sr;
            } else if (target == Long[].class) {
                String[] r = content.split(Pattern.quote(","));
                Long[] sr = new Long[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Long.parseLong(r[i]);
                }
                value = sr;
            } else if (target == Double[].class) {
                String[] r = content.split(Pattern.quote(","));
                Double[] sr = new Double[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Double.parseDouble(r[i]);
                }
                value = sr;
            } else if (target == Short[].class) {
                String[] r = content.split(Pattern.quote(","));
                Short[] sr = new Short[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Short.parseShort(r[i]);
                }
                value = sr;
            } else if (target == Float[].class) {
                String[] r = content.split(Pattern.quote(","));
                Float[] sr = new Float[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Float.parseFloat(r[i]);
                }
                value = sr;
            } else if (target == Byte[].class) {
                String[] r = content.split(Pattern.quote(","));
                Byte[] sr = new Byte[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Byte.parseByte(r[i]);
                }
                value = sr;
            } else if (target == Boolean[].class) {
                String[] r = content.split(Pattern.quote(","));
                Boolean[] sr = new Boolean[r.length];
                for (int i = 0; i < r.length; i++) {
                    sr[i] = Boolean.parseBoolean(r[i]);
                }
                value = sr;
            } else {
                value = serializer.deserialize(content, target);
            }
        }
        return value;
    }

}