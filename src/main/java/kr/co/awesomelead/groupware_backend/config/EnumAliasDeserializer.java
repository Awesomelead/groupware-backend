package kr.co.awesomelead.groupware_backend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;

public class EnumAliasDeserializer extends JsonDeserializer<Object> implements
    ContextualDeserializer {

    private final Class<?> targetType;

    public EnumAliasDeserializer() {
        this.targetType = null;
    }

    public EnumAliasDeserializer(Class<?> targetType) {
        this.targetType = targetType;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String source = p.getValueAsString();
        if (source == null) {
            return null;
        }

        if (targetType == null || !targetType.isEnum()) {
            throw InvalidFormatException.from(
                p,
                "Enum type resolution failed for value: " + source,
                source,
                Object.class);
        }

        try {
            return EnumAliasSupport.parseEnum(targetType, source);
        } catch (IllegalArgumentException e) {
            throw InvalidFormatException.from(p, e.getMessage(), source, targetType);
        }
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
        BeanProperty property) {
        JavaType javaType = property != null ? property.getType() : ctxt.getContextualType();
        if (javaType == null) {
            return this;
        }

        // 1) 필드 자체가 enum인 경우
        Class<?> raw = javaType.getRawClass();
        if (raw != null && raw.isEnum()) {
            return new EnumAliasDeserializer(raw);
        }

        // 2) List<Enum>, Set<Enum> 같은 컨테이너인 경우 원소 타입 확인
        JavaType contentType = javaType.getContentType();
        if (contentType != null) {
            Class<?> contentRaw = contentType.getRawClass();
            if (contentRaw != null && contentRaw.isEnum()) {
                return new EnumAliasDeserializer(contentRaw);
            }
        }

        // enum이 아닌 타입은 이 deserializer 대상이 아님
        return this;
    }
}
