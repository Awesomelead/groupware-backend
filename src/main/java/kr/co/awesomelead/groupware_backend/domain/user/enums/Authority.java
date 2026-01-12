package kr.co.awesomelead.groupware_backend.domain.user.enums;

public enum Authority {
    // 전체 권한은 ADMIN으로 변경할 때, 자동 부여
    WRITE_MESSAGE, // 메세지 작성 권한 -> jobType이 관리직일 경우 부여하고 시작
    WRITE_EDUCATION, // 교육 작성 권한 -> jobType이 관리직일 경우 부여하고 시작
    WRITE_NOTICE, // 공지 작성 권한
    UPLOAD_ANNUAL_LEAVE // 연차 업로드 권한
}
