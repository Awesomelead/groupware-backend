package kr.co.awesomelead.groupware_backend.domain.requesthistory.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import kr.co.awesomelead.groupware_backend.domain.requesthistory.enums.RequestPurpose;

@Converter
public class RequestPurposeConverter implements AttributeConverter<RequestPurpose, String> {

    @Override
    public String convertToDatabaseColumn(RequestPurpose attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public RequestPurpose convertToEntityAttribute(String dbData) {
        return dbData != null ? RequestPurpose.from(dbData) : null;
    }
}
