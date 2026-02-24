package kr.co.awesomelead.groupware_backend.config;

import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;

import org.springframework.core.convert.converter.Converter;

public class StringToRoleConverter implements Converter<String, Role> {

    @Override
    public Role convert(String source) {
        return Role.from(source);
    }
}
