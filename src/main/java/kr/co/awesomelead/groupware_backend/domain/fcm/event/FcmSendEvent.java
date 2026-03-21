package kr.co.awesomelead.groupware_backend.domain.fcm.event;

import java.util.Map;

public record FcmSendEvent(Long userId, String title, String body, Map<String, String> data) {}
