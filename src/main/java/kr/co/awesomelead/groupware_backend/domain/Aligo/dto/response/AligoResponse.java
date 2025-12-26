package kr.co.awesomelead.groupware_backend.domain.aligo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AligoResponse {

    private Integer code;
    private String message;
    private Info info;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Info {

        private String type;
        private String mid;
        private Float current;
        private Float unit;
        private Float total;
        private Integer scnt;
        private Integer fcnt;
    }
}
