package kr.co.awesomelead.groupware_backend.domain.visit.repository;

import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visitor;

public interface CompanionRepository {

    Optional<Visitor> findByPhoneNumberHash(String phoneNumberHash);
}
