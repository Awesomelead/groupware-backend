package kr.co.awesomelead.groupware_backend.global.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import kr.co.awesomelead.groupware_backend.domain.notice.dto.NoticeCompanyJobTypeTargetDto;

import java.util.List;

@Converter
public class NoticeCompanyJobTypeTargetListConverter
        implements AttributeConverter<List<NoticeCompanyJobTypeTargetDto>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<NoticeCompanyJobTypeTargetDto>> TYPE_REFERENCE =
            new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<NoticeCompanyJobTypeTargetDto> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("공지 회사/직군 대상 조건을 JSON으로 변환할 수 없습니다.", e);
        }
    }

    @Override
    public List<NoticeCompanyJobTypeTargetDto> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }

        try {
            return OBJECT_MAPPER.readValue(dbData, TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("공지 회사/직군 대상 조건을 읽을 수 없습니다.", e);
        }
    }
}
