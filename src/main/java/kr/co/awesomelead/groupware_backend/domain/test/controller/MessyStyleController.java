package kr.co.awesomelead.groupware_backend.domain.test.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessyStyleController {

    // 잘못된 들여쓰기와 공백
private String message="Hello World";
    private String name    =   "Test";

    @GetMapping("/messy")
public String getMessyResponse(  ){
if(message!=null){
return message+name;
}else{
return"Error";
}
}

    @GetMapping("/another")
    public String anotherMethod( ) {
        return message;
    }
}