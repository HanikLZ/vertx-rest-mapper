package org.mdvsc.vertx.collection;

import io.vertx.core.MultiMap;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author HanikLZ
 * @since 2017/9/29
 */
public class GenericMultiMap implements MultiMap {

    private final Map<String, List<String>> listMap = new HashMap<>();

    public GenericMultiMap(MultiMap map) {
        addAll(map);
    }

    @Override
    public String get(CharSequence name) {
        return get(safeToString(name));
    }

    @Override
    public String get(String name) {
        List<String> value = listMap.get(name);
        return value != null && !value.isEmpty() ? value.get(0) : null;
    }

    @Override
    public List<String> getAll(String name) {
        return listMap.get(name);
    }

    @Override
    public List<String> getAll(CharSequence name) {
        return getAll(safeToString(name));
    }

    @Override
    public List<Map.Entry<String, String>> entries() {
        return listMap.entrySet().stream().flatMap((Function<Map.Entry<String, List<String>>, Stream<Map.Entry<String, String>>>) stringListEntry -> {
            final String key = stringListEntry.getKey();
            return stringListEntry.getValue().stream().map(s -> new AbstractMap.SimpleEntry<>(key, s));
        }).collect(Collectors.toList());
    }

    @Override
    public boolean contains(String name) {
        return listMap.get(name) != null;
    }

    @Override
    public boolean contains(CharSequence name) {
        return contains(safeToString(name));
    }

    @Override
    public boolean isEmpty() {
        return listMap.isEmpty();
    }

    @Override
    public Set<String> names() {
        return listMap.keySet();
    }

    @Override
    public MultiMap add(String name, String value) {
        listMap.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return this;
    }

    @Override
    public MultiMap add(CharSequence name, CharSequence value) {
        return add(safeToString(name), safeToString(value));
    }

    @Override
    public MultiMap add(String name, Iterable<String> values) {
        List<String> list = listMap.computeIfAbsent(name, k -> new ArrayList<>());
        values.forEach(list::add);
        return this;
    }

    @Override
    public MultiMap add(CharSequence name, Iterable<CharSequence> values) {
        List<String> list = listMap.computeIfAbsent(safeToString(name), k -> new ArrayList<>());
        values.forEach(charSequence -> list.add(safeToString(charSequence)));
        return this;
    }

    @Override
    public MultiMap addAll(MultiMap map) {
        map.forEach(stringStringEntry -> add(stringStringEntry.getKey(), stringStringEntry.getValue()));
        return this;
    }

    @Override
    public MultiMap addAll(Map<String, String> headers) {
        headers.forEach(this::add);
        return this;
    }

    @Override
    public MultiMap set(String name, String value) {
        List<String> list = listMap.computeIfAbsent(name, k -> new ArrayList<>());
        list.clear();
        list.add(value);
        return this;
    }

    @Override
    public MultiMap set(CharSequence name, CharSequence value) {
        return set(name == null ? null : name.toString(), value == null ? null : value.toString());
    }

    @Override
    public MultiMap set(String name, Iterable<String> values) {
        List<String> list = listMap.computeIfAbsent(name, k -> new ArrayList<>());
        list.clear();
        values.forEach(list::add);
        return this;
    }

    @Override
    public MultiMap set(CharSequence name, Iterable<CharSequence> values) {
        List<String> list = listMap.computeIfAbsent(safeToString(name), k -> new ArrayList<>());
        list.clear();
        values.forEach(charSequence -> list.add(safeToString(charSequence)));
        return this;
    }

    @Override
    public MultiMap setAll(MultiMap map) {
        clear();
        addAll(map);
        return this;
    }

    @Override
    public MultiMap setAll(Map<String, String> headers) {
        clear();
        addAll(headers);
        return this;
    }

    @Override
    public MultiMap remove(String name) {
        listMap.remove(name);
        return this;
    }

    @Override
    public MultiMap remove(CharSequence name) {
        return remove(safeToString(name));
    }

    @Override
    public MultiMap clear() {
        listMap.clear();
        return this;
    }

    @Override
    public int size() {
        return listMap.size();
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return entries().iterator();
    }

    private static String safeToString(Object any) {
        return any == null ? null : any.toString();
    }

}
