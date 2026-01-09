package kr.co.awesomelead.groupware_backend.domain.user.enums;

public enum Authority {
    WRITE_MESSAGE, // 메세지 작성 권한
    WRITE_EDUCATION, // 교육 작성 권한
    WRITE_NOTICE, // 공지 작성 권한
    MANAGE_EMPLOYEE_DATA // 연차, 급여명세서, 근태확인표 발송 권한
}
