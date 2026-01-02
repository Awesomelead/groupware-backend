package kr.co.awesomelead.groupware_backend.domain.visit.repository;

import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visitor;

import java.util.Optional;

public interface CompanionRepository {

    Optional<Visitor> findByPhoneNumberHash(String phoneNumberHash);
}
