package kr.co.awesomelead.groupware_backend.domain.approval.mapper;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.BasicApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.CarFuelApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ExpenseDraftApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.LeaveApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.OverseasTripApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.WelfareExpenseApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.Approval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.BasicApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.CarFuelApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.ExpenseDraftApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.LeaveApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.OverseasTripApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.WelfareExpenseApproval;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.SubclassMapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApprovalMapper {

    @BeanMapping(subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION)
    @SubclassMapping(source = LeaveApprovalCreateRequestDto.class, target = LeaveApproval.class)
    @SubclassMapping(source = CarFuelApprovalCreateRequestDto.class, target = CarFuelApproval.class)
    @SubclassMapping(
            source = WelfareExpenseApprovalCreateRequestDto.class,
            target = WelfareExpenseApproval.class)
    @SubclassMapping(
            source = ExpenseDraftApprovalCreateRequestDto.class,
            target = ExpenseDraftApproval.class)
    @SubclassMapping(
            source = OverseasTripApprovalCreateRequestDto.class,
            target = OverseasTripApproval.class)
    @SubclassMapping(source = BasicApprovalCreateRequestDto.class, target = BasicApproval.class)
    Approval toEntity(ApprovalCreateRequestDto dto);

    @Mapping(target = "drafter", ignore = true)
    @Mapping(target = "draftDepartment", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "steps", ignore = true)
    LeaveApproval toLeaveEntity(LeaveApprovalCreateRequestDto dto);

    @Mapping(target = "drafter", ignore = true)
    @Mapping(target = "draftDepartment", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "steps", ignore = true)
    CarFuelApproval toCarFuelEntity(CarFuelApprovalCreateRequestDto dto);

    @Mapping(target = "drafter", ignore = true)
    @Mapping(target = "draftDepartment", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "steps", ignore = true)
    ExpenseDraftApproval toExpenseEntity(ExpenseDraftApprovalCreateRequestDto dto);

    @Mapping(target = "drafter", ignore = true)
    @Mapping(target = "draftDepartment", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "steps", ignore = true)
    OverseasTripApproval toOverseasEntity(OverseasTripApprovalCreateRequestDto dto);

    @Mapping(target = "drafter", ignore = true)
    @Mapping(target = "draftDepartment", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "steps", ignore = true)
    WelfareExpenseApproval toWelfareEntity(WelfareExpenseApprovalCreateRequestDto dto);

    @Mapping(target = "drafter", ignore = true)
    @Mapping(target = "draftDepartment", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "steps", ignore = true)
    BasicApproval toBasicEntity(BasicApprovalCreateRequestDto dto);
}
