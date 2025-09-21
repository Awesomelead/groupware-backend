package kr.co.awesomelead.groupware_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CiTestExample {

    @Test
    void successfulTest() {
        // 성공하는 테스트
        int result = 10 + 5;
        assertEquals(15, result);
    }

    @Test
    void intentionalFailTest() {
        // CI 실패 테스트용
        assertEquals(1, 2, "이 테스트는 의도적으로 실패합니다");
    }
}
