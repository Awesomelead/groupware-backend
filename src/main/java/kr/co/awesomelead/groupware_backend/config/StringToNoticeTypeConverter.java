package kr.co.awesomelead.groupware_backend.config;

import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeType;

import org.springframework.core.convert.converter.Converter;

public class StringToNoticeTypeConverter implements Converter<String, NoticeType> {

    @Override
    public NoticeType convert(String source) {
        return NoticeType.from(source);
    }
}
