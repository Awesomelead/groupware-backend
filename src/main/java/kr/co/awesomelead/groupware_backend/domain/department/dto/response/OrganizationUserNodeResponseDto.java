package kr.co.awesomelead.groupware_backend.domain.department.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "조직도 사용자 노드")
public class OrganizationUserNodeResponseDto {

    @Schema(description = "사용자 ID", example = "17")
    private Long id;

    @Schema(description = "사용자명", example = "고영민")
    private String name;

    @Schema(description = "직급", example = "대리")
    private Position position;

    @Schema(description = "사용자 상태", example = "AVAILABLE")
    private Status status;

    public static OrganizationUserNodeResponseDto from(User user) {
        return OrganizationUserNodeResponseDto.builder()
                .id(user.getId())
                .name(user.getDisplayName())
                .position(user.getPosition())
                .status(user.getStatus())
                .build();
    }
}
