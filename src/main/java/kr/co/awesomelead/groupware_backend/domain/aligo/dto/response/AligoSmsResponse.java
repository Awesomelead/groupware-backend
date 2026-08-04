package kr.co.awesomelead.groupware_backend.domain.aligo.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AligoSmsResponse {

    @JsonProperty("result_code")
    private Integer resultCode;

    private String message;

    @JsonProperty("msg_id")
    private Long messageId;

    @JsonProperty("success_cnt")
    private Integer successCount;

    @JsonProperty("error_cnt")
    private Integer errorCount;

    @JsonProperty("msg_type")
    private String messageType;
}
