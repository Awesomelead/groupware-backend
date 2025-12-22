package kr.co.awesomelead.groupware_backend.domain.leaverequest.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LeaveType {
    LEAVE("휴가"),          // 소분류 필요 (연차, 경조, 생리, 유급, 무급)
    HALF_OFF("반차"),       // 소분류 필요 (오전, 오후)
    EDUCATION("교육"),      // 소분류 불필요 (NULL)
    TRAINING("훈련(예비군)"), // 소분류 불필요 (NULL)
    OTHER("기타");          // 소분류 불필요 (NULL)

    private final String description;
}
