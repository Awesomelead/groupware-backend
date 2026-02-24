package kr.co.awesomelead.groupware_backend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;

public class EnumAliasDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {

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
            throw InvalidFormatException.from(p, "Enum target type is not resolved", source, Object.class);
        }
        try {
            return EnumAliasSupport.parseEnum(targetType, source);
        } catch (IllegalArgumentException e) {
            throw InvalidFormatException.from(p, e.getMessage(), source, targetType);
        }
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        JavaType javaType = property != null ? property.getType() : ctxt.getContextualType();
        if (javaType == null) {
            return this;
        }
        Class<?> raw = javaType.getRawClass();
        if (!raw.isEnum()) {
            return this;
        }
        return new EnumAliasDeserializer(raw);
    }
}
