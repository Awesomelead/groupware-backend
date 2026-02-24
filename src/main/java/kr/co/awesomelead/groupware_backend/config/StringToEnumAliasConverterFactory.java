package kr.co.awesomelead.groupware_backend.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

public class StringToEnumAliasConverterFactory implements ConverterFactory<String, Enum> {

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
        return source -> (T) EnumAliasSupport.parseEnum(targetType, source);
    }
}
