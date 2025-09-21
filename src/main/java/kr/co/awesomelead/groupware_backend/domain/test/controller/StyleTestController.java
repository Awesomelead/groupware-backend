package kr.co.awesomelead.groupware_backend.domain.test.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StyleTestController {

private String message = "안녕하세요";

@GetMapping("/test")
public String test() {
    return message;
}
}