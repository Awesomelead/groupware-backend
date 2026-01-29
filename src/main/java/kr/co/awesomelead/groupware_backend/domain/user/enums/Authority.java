package kr.co.awesomelead.groupware_backend.domain.user.enums;

public enum Authority {
    // 전체 권한은 ADMIN으로 변경할 때, 자동 부여
    ACCESS_MESSAGE, // 메세지 작성 권한 -> jobType이 관리직일 경우 부여하고 시작
    ACCESS_EDUCATION, // 교육 작성 권한 -> jobType이 관리직일 경우 부여하고 시작
    ACCESS_NOTICE, // 공지 작성 권한

    ACCESS_VISIT, // 방문자 관리 시스템 접근 권한

    MANAGE_EMPLOYEE_DATA // 연차, 급여명세서, 근태확인표 발송 권한
}
