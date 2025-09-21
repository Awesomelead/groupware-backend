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
}
