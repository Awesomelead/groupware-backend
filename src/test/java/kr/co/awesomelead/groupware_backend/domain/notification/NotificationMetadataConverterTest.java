package kr.co.awesomelead.groupware_backend.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;

import kr.co.awesomelead.groupware_backend.global.util.NotificationMetadataConverter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class NotificationMetadataConverterTest {

    private final NotificationMetadataConverter converter = new NotificationMetadataConverter();

    @Test
    @DisplayName("Map을 JSON 문자열로 직렬화한다")
    void convertToDatabaseColumn_returnsJsonString() {
        Map<String, Object> metadata = Map.of("requestId", 10, "status", "PENDING");

        String result = converter.convertToDatabaseColumn(metadata);

        assertThat(result).isNotNull();
        assertThat(result).contains("requestId");
        assertThat(result).contains("PENDING");
    }

    @Test
    @DisplayName("null 또는 빈 Map은 null을 반환한다")
    void convertToDatabaseColumn_returnsNull_whenNullOrEmpty() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToDatabaseColumn(Map.of())).isNull();
    }

    @Test
    @DisplayName("JSON 문자열을 Map으로 역직렬화한다")
    void convertToEntityAttribute_returnsMap() {
        String json = "{\"requestId\":10,\"status\":\"PENDING\"}";

        Map<String, Object> result = converter.convertToEntityAttribute(json);

        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("PENDING");
        assertThat(((Number) result.get("requestId")).intValue()).isEqualTo(10);
    }

    @Test
    @DisplayName("null 또는 빈 문자열은 null을 반환한다")
    void convertToEntityAttribute_returnsNull_whenNullOrBlank() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("")).isNull();
        assertThat(converter.convertToEntityAttribute("   ")).isNull();
    }

    @Test
    @DisplayName("직렬화 후 역직렬화하면 원래 값과 동일하다")
    void roundTrip_preservesValues() {
        Map<String, Object> original = Map.of("approvalTargetId", 42L, "isApprovalTarget", true);

        String json = converter.convertToDatabaseColumn(original);
        Map<String, Object> restored = converter.convertToEntityAttribute(json);

        assertThat(restored).isNotNull();
        assertThat(((Number) restored.get("approvalTargetId")).longValue()).isEqualTo(42L);
        assertThat(restored.get("isApprovalTarget")).isEqualTo(true);
    }
}
