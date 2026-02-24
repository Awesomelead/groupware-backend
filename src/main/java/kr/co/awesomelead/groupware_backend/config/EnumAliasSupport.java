package kr.co.awesomelead.groupware_backend.config;

import com.fasterxml.jackson.annotation.JsonValue;

import java.lang.reflect.Method;
import java.util.Arrays;

public final class EnumAliasSupport {

    private EnumAliasSupport() {}

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> E parseEnum(Class<?> rawEnumClass, String source) {
        if (rawEnumClass == null || source == null) {
            throw new IllegalArgumentException("Enum class and source must not be null");
        }
        if (!rawEnumClass.isEnum()) {
            throw new IllegalArgumentException(rawEnumClass.getName() + " is not enum");
        }

        String normalized = source.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Enum source must not be blank");
        }

        Class<E> enumClass = (Class<E>) rawEnumClass;

        for (E constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(normalized)) {
                return constant;
            }
        }

        Method jsonValueMethod = findJsonValueMethod(enumClass);
        if (jsonValueMethod != null) {
            for (E constant : enumClass.getEnumConstants()) {
                String alias = invokeToString(jsonValueMethod, constant);
                if (alias != null && alias.equalsIgnoreCase(normalized)) {
                    return constant;
                }
            }
        }

        Method descriptionMethod = findDescriptionMethod(enumClass);
        if (descriptionMethod != null) {
            for (E constant : enumClass.getEnumConstants()) {
                String alias = invokeToString(descriptionMethod, constant);
                if (alias != null && alias.equalsIgnoreCase(normalized)) {
                    return constant;
                }
            }
        }

        throw new IllegalArgumentException(
                "Unknown enum value '" + source + "' for " + enumClass.getSimpleName());
    }

    private static Method findJsonValueMethod(Class<?> enumClass) {
        return Arrays.stream(enumClass.getMethods())
                .filter(m -> m.getParameterCount() == 0 && m.isAnnotationPresent(JsonValue.class))
                .findFirst()
                .orElse(null);
    }

    private static Method findDescriptionMethod(Class<?> enumClass) {
        try {
            Method method = enumClass.getMethod("getDescription");
            return method.getParameterCount() == 0 ? method : null;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static String invokeToString(Method method, Object target) {
        try {
            Object value = method.invoke(target);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }
}
