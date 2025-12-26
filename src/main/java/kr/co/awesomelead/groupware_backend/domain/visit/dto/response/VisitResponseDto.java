package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VisitResponseDto {

    private Long id;

    // 담당자 정보
    private Long hostUserId;
    private String hostName;
    private String hostDepartment; // 필요하다면

    // 방문자 정보
    private String visitorName;
    private String visitorCompany;
    private String visitorPhone;

    // 방문 내용
    private VisitType visitType; //
    private VisitPurpose purpose;
    private String carNumber;

    // 시간 및 상태
    private LocalDateTime visitStartDate;
    private LocalDateTime visitEndDate;
    private boolean visited;   // 실제 방문 완료 여부
    private boolean verified;  // 신원 확인 여부

    // 동행자 리스트 (Response용 DTO를 따로 내부 클래스로 써도 됨)
    private List<CompanionResponseDto> companions;

    // Entity -> DTO 변환 메서드 (Factory Method)
    public static VisitResponseDto from(Visit visit) {
        return VisitResponseDto.builder()
            .id(visit.getId())
            .hostUserId(visit.getUser().getId())
            .hostName(visit.getUser().getNameKor()) // User에 name이 있다고 가정
            // .hostDepartment(entity.getUser().getDepartment().getName())

            .visitorName(visit.getVisitor().getName())
            .visitorCompany(visit.getVisitorCompany()) // 혹은 visitor.getCompany() 등 설계에 따라
            .visitorPhone(visit.getVisitor().getPhoneNumber())

            .visitType(visit.getVisitType())
            .purpose(visit.getPurpose())
            .carNumber(visit.getCarNumber())

            .visitStartDate(visit.getVisitStartDate())
            .visitEndDate(visit.getVisitEndDate())
            .visited(visit.isVisited())
            .verified(visit.isVerified())

            .companions(visit.getCompanions().stream()
                .map(CompanionResponseDto::from)
                .collect(Collectors.toList()))
            .build();
    }

    // 내부 클래스로 동행자 응답 DTO 정의
    @Getter
    @Builder
    public static class CompanionResponseDto {

        private String name;
        private String phoneNumber;

        public static CompanionResponseDto from(
            kr.co.awesomelead.groupware_backend.domain.visit.entity.Companion entity) {
            return CompanionResponseDto.builder()
                .name(entity.getName())
                .phoneNumber(entity.getPhoneNumber())
                .build();
        }
    }

}
