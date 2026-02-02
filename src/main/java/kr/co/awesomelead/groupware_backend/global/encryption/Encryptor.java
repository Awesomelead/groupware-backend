package kr.co.awesomelead.groupware_backend.global.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Converter
@Component
@RequiredArgsConstructor
public class Encryptor implements AttributeConverter<String, String> {

    private final AESEncryptor aesEncryptor;

    // Entity → DB 저장 시 호출 (암호화)
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return aesEncryptor.encrypt(attribute);
    }

    // DB → Entity 조회 시 호출 (복호화)
    @Override
    public String convertToEntityAttribute(String dbData) {
        return aesEncryptor.decrypt(dbData);
    }
}
