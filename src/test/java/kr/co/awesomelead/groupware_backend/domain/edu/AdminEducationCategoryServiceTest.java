package kr.co.awesomelead.groupware_backend.domain.edu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.domain.education.dto.response.AdminEducationCategoryNodeDto;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EducationCategory;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EducationCategoryType;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EducationCategoryRepository;
import kr.co.awesomelead.groupware_backend.domain.education.service.AdminEducationCategoryService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AdminEducationCategoryServiceTest {

    @Mock private EducationCategoryRepository educationCategoryRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private AdminEducationCategoryService adminEducationCategoryService;

    @Test
    void 관리자_조회는_비활성_카테고리를_포함한_트리를_반환한다() {
        Long adminId = 1L;
        User admin = User.builder().id(adminId).role(Role.ADMIN).build();
        EducationCategory parent =
                EducationCategory.builder()
                        .id(10L)
                        .code("SAFETY_RESOURCE")
                        .name("안전보건 자료")
                        .categoryType(EducationCategoryType.SAFETY)
                        .depth(0)
                        .active(false)
                        .build();
        EducationCategory child =
                EducationCategory.builder()
                        .id(11L)
                        .code("SAFETY_GUIDE")
                        .name("안전보건 지침")
                        .categoryType(EducationCategoryType.SAFETY)
                        .parent(parent)
                        .depth(1)
                        .active(true)
                        .build();

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(educationCategoryRepository
                        .findAllByCategoryTypeOrderByDepthAscSortOrderAscIdAsc(
                                EducationCategoryType.SAFETY))
                .thenReturn(List.of(parent, child));

        List<AdminEducationCategoryNodeDto> result =
                adminEducationCategoryService.getCategoryTree(
                        adminId, EducationCategoryType.SAFETY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isActive()).isFalse();
        assertThat(result.get(0).getChildren()).hasSize(1);
        assertThat(result.get(0).getChildren().get(0).isActive()).isTrue();
    }

    @Test
    void 일반_사용자는_관리자용_카테고리를_조회할_수_없다() {
        Long userId = 2L;
        User user = User.builder().id(userId).role(Role.USER).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(
                        () ->
                                adminEducationCategoryService.getCategoryTree(
                                        userId, EducationCategoryType.SAFETY))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", ErrorCode.NO_AUTHORITY_FOR_EDUCATION_CATEGORY_MANAGE);

        verify(userRepository).findById(userId);
    }
}
