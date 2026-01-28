package kr.co.awesomelead.groupware_backend.global.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class CompanyListConverter implements AttributeConverter<List<Company>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<Company> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        // Company.name()을 호출하므로 @JsonValue와 상관없이 "AWESOME,MARUI"로 저장됨
        return attribute.stream().map(Company::name).collect(Collectors.joining(DELIMITER));
    }

    @Override
    public List<Company> convertToEntityAttribute(String dbData) {
        if (!StringUtils.hasText(dbData)) {
            return List.of();
        }
        // DB의 "AWESOME" 문자열을 읽어 Company.AWESOME 상수로 변환
        return Arrays.stream(dbData.split(DELIMITER))
                .map(Company::valueOf)
                .collect(Collectors.toList());
    }
}
