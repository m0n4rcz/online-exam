package shared;

import java.lang.reflect.*;
import java.util.*;

/**
 * Lightweight, zero-dependency JSON serializer and deserializer for Java 17.
 * Enables seamless JSON protocol transmission across pure Java TCP sockets
 * without requiring external libraries (like Gson or Jackson).
 */
public class JsonUtil {

    private JsonUtil() {}

    // ==========================================
    // SERIALIZATION (Object -> JSON String)
    // ==========================================

    public static String toJson(Object obj) {
        if (obj == null) return "null";

        if (obj instanceof String s) {
            return quoteString(s);
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Character c) {
            return quoteString(c.toString());
        }
        if (obj instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append(quoteString(String.valueOf(entry.getKey())));
                sb.append(":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Iterable<?> iter) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : iter) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj.getClass().isArray()) {
            StringBuilder sb = new StringBuilder("[");
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(Array.get(obj, i)));
            }
            sb.append("]");
            return sb.toString();
        }

        // Generic POJO / DTO
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        List<Field> fields = getAllFields(obj.getClass());
        for (Field f : fields) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                continue;
            }
            f.setAccessible(true);
            try {
                Object val = f.get(obj);
                if (!first) sb.append(",");
                sb.append(quoteString(f.getName()));
                sb.append(":");
                sb.append(toJson(val));
                first = false;
            } catch (Exception ignored) {}
        }
        sb.append("}");
        return sb.toString();
    }

    private static String quoteString(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> list = new ArrayList<>();
        Class<?> curr = clazz;
        while (curr != null && curr != Object.class) {
            list.addAll(Arrays.asList(curr.getDeclaredFields()));
            curr = curr.getSuperclass();
        }
        return list;
    }

    // ==========================================
    // DESERIALIZATION (JSON String -> Object / POJO)
    // ==========================================

    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty() || json.trim().equals("null")) {
            return null;
        }
        Object parsed = parse(json);
        if (parsed == null) return null;

        if (clazz.isInstance(parsed)) {
            return (T) parsed;
        }
        if (parsed instanceof Map<?, ?> map) {
            return mapToPojo((Map<String, Object>) map, clazz);
        }
        return null;
    }

    public static Map<String, Object> parseMap(String json) {
        Object parsed = parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) map;
            return m;
        }
        return Collections.emptyMap();
    }

    public static List<Object> parseList(String json) {
        Object parsed = parse(json);
        if (parsed instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<Object> l = (List<Object>) list;
            return l;
        }
        return Collections.emptyList();
    }

    public static <T> List<T> fromJsonList(String json, Class<T> itemClass) {
        List<Object> rawList = parseList(json);
        List<T> result = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                T obj = mapToPojo((Map<String, Object>) map, itemClass);
                result.add(obj);
            } else if (itemClass.isInstance(item)) {
                result.add(itemClass.cast(item));
            }
        }
        return result;
    }

    public static Object parse(String json) {
        if (json == null) return null;
        json = json.trim();
        if (json.isEmpty() || json.equals("null")) return null;
        return new Parser(json).parseValue();
    }

    @SuppressWarnings("unchecked")
    private static <T> T mapToPojo(Map<String, Object> map, Class<T> clazz) {
        try {
            Constructor<T> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            T instance = ctor.newInstance();

            List<Field> fields = getAllFields(clazz);
            for (Field f : fields) {
                if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                    continue;
                }
                String name = f.getName();
                if (!map.containsKey(name)) continue;

                Object raw = map.get(name);
                f.setAccessible(true);
                Object converted = convertValue(raw, f.getType(), f.getGenericType());
                f.set(instance, converted);
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate " + clazz.getName() + ": " + e.getMessage(), e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convertValue(Object raw, Class<?> targetType, Type genericType) {
        if (raw == null) {
            if (targetType.isPrimitive()) {
                if (targetType == boolean.class) return false;
                if (targetType == int.class) return 0;
                if (targetType == long.class) return 0L;
                if (targetType == double.class) return 0.0;
                if (targetType == float.class) return 0.0f;
            }
            return null;
        }

        if (targetType == String.class) {
            return String.valueOf(raw);
        }
        if (targetType == int.class || targetType == Integer.class) {
            if (raw instanceof Number n) return n.intValue();
            return Integer.parseInt(raw.toString());
        }
        if (targetType == long.class || targetType == Long.class) {
            if (raw instanceof Number n) return n.longValue();
            return Long.parseLong(raw.toString());
        }
        if (targetType == double.class || targetType == Double.class) {
            if (raw instanceof Number n) return n.doubleValue();
            return Double.parseDouble(raw.toString());
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (raw instanceof Boolean b) return b;
            return Boolean.parseBoolean(raw.toString());
        }
        if (targetType == short.class || targetType == Short.class) {
            if (raw instanceof Number n) return n.shortValue();
            return Short.parseShort(raw.toString());
        }

        // List conversion
        if (List.class.isAssignableFrom(targetType) && raw instanceof List<?> rawList) {
            Type itemType = Object.class;
            if (genericType instanceof ParameterizedType pt) {
                Type[] args = pt.getActualTypeArguments();
                if (args.length > 0) itemType = args[0];
            }
            List<Object> resultList = new ArrayList<>();
            Class<?> itemClass = (itemType instanceof Class<?>) ? (Class<?>) itemType : Object.class;
            for (Object item : rawList) {
                resultList.add(convertValue(item, itemClass, itemType));
            }
            return resultList;
        }

        // Map conversion
        if (Map.class.isAssignableFrom(targetType) && raw instanceof Map<?, ?> rawMap) {
            Type keyType = String.class;
            Type valType = Object.class;
            if (genericType instanceof ParameterizedType pt) {
                Type[] args = pt.getActualTypeArguments();
                if (args.length > 1) {
                    keyType = args[0];
                    valType = args[1];
                }
            }
            Map<Object, Object> resultMap = new HashMap<>();
            Class<?> keyClass = (keyType instanceof Class<?>) ? (Class<?>) keyType : String.class;
            Class<?> valClass = (valType instanceof Class<?>) ? (Class<?>) valType : Object.class;

            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                Object convertedKey = convertKey(entry.getKey().toString(), keyClass);
                Object convertedVal = convertValue(entry.getValue(), valClass, valType);
                resultMap.put(convertedKey, convertedVal);
            }
            return resultMap;
        }

        // Nested POJO
        if (raw instanceof Map<?, ?> rawMap) {
            return mapToPojo((Map<String, Object>) rawMap, targetType);
        }

        return raw;
    }

    private static Object convertKey(String key, Class<?> keyClass) {
        if (keyClass == Integer.class || keyClass == int.class) {
            return Integer.parseInt(key);
        }
        if (keyClass == Long.class || keyClass == long.class) {
            return Long.parseLong(key);
        }
        return key;
    }

    // ==========================================
    // RECURSIVE-DESCENT PARSER IMPLEMENTATION
    // ==========================================

    private static class Parser {
        private final String src;
        private int idx = 0;

        Parser(String src) {
            this.src = src;
        }

        Object parseValue() {
            skipWhitespace();
            if (idx >= src.length()) return null;

            char c = src.charAt(idx);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();

            throw new IllegalArgumentException("Unexpected char '" + c + "' at pos " + idx);
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            idx++; // consume '{'
            skipWhitespace();
            if (idx < src.length() && src.charAt(idx) == '}') {
                idx++;
                return map;
            }

            while (idx < src.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (idx >= src.length() || src.charAt(idx) != ':') {
                    throw new IllegalArgumentException("Expected ':' at pos " + idx);
                }
                idx++; // consume ':'
                Object val = parseValue();
                map.put(key, val);

                skipWhitespace();
                if (idx < src.length() && src.charAt(idx) == ',') {
                    idx++; // consume ','
                } else if (idx < src.length() && src.charAt(idx) == '}') {
                    idx++; // consume '}'
                    break;
                } else {
                    throw new IllegalArgumentException("Expected ',' or '}' at pos " + idx);
                }
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            idx++; // consume '['
            skipWhitespace();
            if (idx < src.length() && src.charAt(idx) == ']') {
                idx++;
                return list;
            }

            while (idx < src.length()) {
                Object val = parseValue();
                list.add(val);
                skipWhitespace();
                if (idx < src.length() && src.charAt(idx) == ',') {
                    idx++; // consume ','
                } else if (idx < src.length() && src.charAt(idx) == ']') {
                    idx++; // consume ']'
                    break;
                } else {
                    throw new IllegalArgumentException("Expected ',' or ']' at pos " + idx);
                }
            }
            return list;
        }

        private String parseString() {
            if (src.charAt(idx) != '"') {
                throw new IllegalArgumentException("Expected '\"' at pos " + idx);
            }
            idx++; // consume opening quote
            StringBuilder sb = new StringBuilder();
            while (idx < src.length()) {
                char c = src.charAt(idx++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (idx >= src.length()) break;
                    char esc = src.charAt(idx++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (idx + 4 <= src.length()) {
                                String hex = src.substring(idx, idx + 4);
                                sb.append((char) Integer.parseInt(hex, 16));
                                idx += 4;
                            }
                        }
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Boolean parseBoolean() {
            if (src.startsWith("true", idx)) {
                idx += 4;
                return Boolean.TRUE;
            }
            if (src.startsWith("false", idx)) {
                idx += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid boolean literal at pos " + idx);
        }

        private Object parseNull() {
            if (src.startsWith("null", idx)) {
                idx += 4;
                return null;
            }
            throw new IllegalArgumentException("Invalid null literal at pos " + idx);
        }

        private Number parseNumber() {
            int start = idx;
            if (src.charAt(idx) == '-') idx++;
            boolean isFloating = false;
            while (idx < src.length()) {
                char c = src.charAt(idx);
                if ((c >= '0' && c <= '9')) {
                    idx++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '+') {
                    isFloating = true;
                    idx++;
                } else {
                    break;
                }
            }
            String numStr = src.substring(start, idx);
            if (isFloating) {
                return Double.parseDouble(numStr);
            } else {
                try {
                    return Integer.parseInt(numStr);
                } catch (NumberFormatException e) {
                    return Long.parseLong(numStr);
                }
            }
        }

        private void skipWhitespace() {
            while (idx < src.length()) {
                char c = src.charAt(idx);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    idx++;
                } else {
                    break;
                }
            }
        }
    }
}
