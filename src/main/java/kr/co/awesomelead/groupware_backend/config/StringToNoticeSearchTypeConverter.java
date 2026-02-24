package kr.co.awesomelead.groupware_backend.config;

import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeSearchType;

import org.springframework.core.convert.converter.Converter;

public class StringToNoticeSearchTypeConverter implements Converter<String, NoticeSearchType> {

    @Override
    public NoticeSearchType convert(String source) {
        return NoticeSearchType.from(source);
    }
}
