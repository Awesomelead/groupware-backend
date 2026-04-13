package kr.co.awesomelead.groupware_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class SwaggerConfig {

    private static final Pattern HAS_ROLE_PATTERN = Pattern.compile("hasRole\\('([^']+)'\\)");
    private static final Pattern HAS_ANY_ROLE_PATTERN = Pattern.compile("hasAnyRole\\(([^)]*)\\)");
    private static final Pattern HAS_AUTHORITY_PATTERN =
            Pattern.compile("hasAuthority\\('([^']+)'\\)");
    private static final Pattern HAS_ANY_AUTHORITY_PATTERN =
            Pattern.compile("hasAnyAuthority\\(([^)]*)\\)");
    private static final Pattern QUOTED_TOKEN_PATTERN = Pattern.compile("'([^']+)'");
    private static final List<String> PUBLIC_PATH_PATTERNS =
            List.of(
                    "/",
                    "/index.html",
                    "/api/test/**",
                    "/api/auth/login",
                    "/api/auth/signup",
                    "/api/auth/bootstrap-promote-admin",
                    "/api/auth/reissue",
                    "/api/auth/verify-phone-code",
                    "/api/auth/verify-account/email",
                    "/api/auth/verify-account/phone",
                    "/api/auth/verify-identity",
                    "/api/auth/verify-email-code",
                    "/api/auth/send-phone-code",
                    "/api/auth/send-email-code",
                    "/api/auth/find-email",
                    "/api/auth/reset-password/phone",
                    "/api/auth/reset-password/email",
                    "/api/educations/attachments/{id}/download",
                    "/api/departments/hierarchy",
                    "/api/departments/{departmentId}/users",
                    "/api/visits/**",
                    "/api/users/list");

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info().title("어썸리드 API").version("v1.0.0").description("어썸리드 API 입니다");

        // JWT 인증 스키마 정의
        SecurityScheme securityScheme =
                new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization");

        // Security Requirement 정의
        SecurityRequirement securityRequirement =
                new SecurityRequirement().addList("Bearer Authentication");

        return new OpenAPI()
                .components(
                        new Components()
                                .addSecuritySchemes("Bearer Authentication", securityScheme))
                .addSecurityItem(securityRequirement)
                .addServersItem(new Server().url("/"))
                .info(info);
    }

    @Bean
    public OperationCustomizer operationDocCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            String resolvedPath = resolveApiPath(handlerMethod);
            String httpMethod = resolveHttpMethod(handlerMethod);
            ensureSummary(operation, handlerMethod, httpMethod, resolvedPath);
            ensureTag(operation, handlerMethod);

            String authoritySection = buildAuthoritySection(handlerMethod);
            String enumSection = buildEnumSectionForCurrentApi(handlerMethod);
            boolean isPublicApi = isPublicApiPath(resolvedPath);
            String uploadSection = buildUploadSection(handlerMethod);

            String baseDescription = operation.getDescription();
            if (baseDescription == null || baseDescription.isBlank()) {
                baseDescription = String.format("엔드포인트: `%s %s`", httpMethod, resolvedPath);
            }

            StringBuilder append = new StringBuilder();
            append.append("\n\n---\n### 권한 정보\n").append(authoritySection);
            append.append("\n\n### 사용 Enum\n").append(enumSection);
            if (uploadSection != null) {
                append.append("\n\n### 파일 업로드\n").append(uploadSection);
            }

            operation.setDescription(baseDescription + append);

            addDefaultErrorResponses(operation, isPublicApi);

            return operation;
        };
    }

    private void ensureSummary(
            Operation operation, HandlerMethod handlerMethod, String httpMethod, String path) {
        if (operation.getSummary() != null && !operation.getSummary().isBlank()) {
            return;
        }
        String methodName = handlerMethod.getMethod().getName();
        operation.setSummary(String.format("[%s] %s", httpMethod, humanizeMethodName(methodName)));
    }

    private void ensureTag(Operation operation, HandlerMethod handlerMethod) {
        if (operation.getTags() != null && !operation.getTags().isEmpty()) {
            return;
        }
        String controllerName = handlerMethod.getBeanType().getSimpleName();
        if (controllerName.endsWith("Controller")) {
            controllerName =
                    controllerName.substring(0, controllerName.length() - "Controller".length());
        }
        operation.setTags(List.of(controllerName));
    }

    private String buildUploadSection(HandlerMethod handlerMethod) {
        boolean hasMultipartFile =
                Arrays.stream(handlerMethod.getMethod().getParameterTypes())
                        .anyMatch(MultipartFile.class::isAssignableFrom);

        if (!hasMultipartFile) {
            for (Type type : handlerMethod.getMethod().getGenericParameterTypes()) {
                if (type instanceof ParameterizedType parameterizedType) {
                    for (Type argType : parameterizedType.getActualTypeArguments()) {
                        if (argType == MultipartFile.class) {
                            hasMultipartFile = true;
                            break;
                        }
                    }
                }
                if (hasMultipartFile) {
                    break;
                }
            }
        }

        if (!hasMultipartFile) {
            return null;
        }
        return "- `multipart/form-data` 요청\n" + "- 파일당 최대 `50MB`, 요청당 최대 `100MB` (환경 설정값 기준)";
    }

    private void addDefaultErrorResponses(Operation operation, boolean isPublicApi) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }

        putIfAbsent(responses, "400", "요청 값 오류", "COMMON400", "입력값이 유효하지 않습니다.");
        if (!isPublicApi) {
            putIfAbsent(responses, "401", "인증 실패", "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
            putIfAbsent(responses, "403", "권한 부족", "NO_AUTHORITY_FOR_XXX", "요청 권한이 없습니다.");
        }
        putIfAbsent(responses, "404", "리소스 없음", "XXX_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.");
        putIfAbsent(responses, "409", "데이터 충돌", "COMMON409", "데이터 무결성 위반이 발생했습니다. (중복 데이터 등)");
        putIfAbsent(responses, "500", "서버 내부 오류", "COMMON500", "서버 내부 오류가 발생했습니다.");
    }

    private void putIfAbsent(
            ApiResponses responses,
            String statusCode,
            String description,
            String errorCode,
            String errorMessage) {
        if (responses.containsKey(statusCode)) {
            return;
        }

        Map<String, Object> example = new LinkedHashMap<>();
        example.put("isSuccess", false);
        example.put("code", errorCode);
        example.put("message", errorMessage);
        example.put("result", null);

        io.swagger.v3.oas.models.responses.ApiResponse apiResponse =
                new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description(description)
                        .content(
                                new Content()
                                        .addMediaType(
                                                "application/json",
                                                new MediaType()
                                                        .schema(new ObjectSchema())
                                                        .example(example)));
        responses.addApiResponse(statusCode, apiResponse);
    }

    private String buildAuthoritySection(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        PreAuthorize methodAuth = method.getAnnotation(PreAuthorize.class);
        PreAuthorize classAuth = handlerMethod.getBeanType().getAnnotation(PreAuthorize.class);

        String expression =
                methodAuth != null
                        ? methodAuth.value()
                        : classAuth != null ? classAuth.value() : null;

        if (expression == null || expression.isBlank()) {
            String resolvedPath = resolveApiPath(handlerMethod);
            if (isPublicApiPath(resolvedPath)) {
                return "- 공개 API (권한 불필요)";
            }
            return "- 로그인 필요\n- 명시된 `@PreAuthorize` 없음 (서비스 레이어 권한 검증)";
        }

        List<String> roles =
                parseAuthorityTokens(expression, HAS_ROLE_PATTERN, HAS_ANY_ROLE_PATTERN);
        List<String> authorities =
                parseAuthorityTokens(expression, HAS_AUTHORITY_PATTERN, HAS_ANY_AUTHORITY_PATTERN);

        StringBuilder result = new StringBuilder();
        result.append("- `@PreAuthorize`: `").append(expression).append("`");

        if (!roles.isEmpty()) {
            result.append("\n- 필요 Role: ").append(String.join(", ", roles));
        }
        if (!authorities.isEmpty()) {
            result.append("\n- 필요 Authority: ").append(String.join(", ", authorities));
        }
        return result.toString();
    }

    private String resolveApiPath(HandlerMethod handlerMethod) {
        String classPath = "";
        RequestMapping classMapping =
                handlerMethod.getBeanType().getAnnotation(RequestMapping.class);
        if (classMapping != null && classMapping.value().length > 0) {
            classPath = classMapping.value()[0];
        }

        String methodPath = "";
        Method method = handlerMethod.getMethod();
        if (method.isAnnotationPresent(GetMapping.class)) {
            String[] values = method.getAnnotation(GetMapping.class).value();
            methodPath = values.length > 0 ? values[0] : "";
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            String[] values = method.getAnnotation(PostMapping.class).value();
            methodPath = values.length > 0 ? values[0] : "";
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            String[] values = method.getAnnotation(PutMapping.class).value();
            methodPath = values.length > 0 ? values[0] : "";
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            String[] values = method.getAnnotation(PatchMapping.class).value();
            methodPath = values.length > 0 ? values[0] : "";
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            String[] values = method.getAnnotation(DeleteMapping.class).value();
            methodPath = values.length > 0 ? values[0] : "";
        } else if (method.isAnnotationPresent(RequestMapping.class)) {
            String[] values = method.getAnnotation(RequestMapping.class).value();
            methodPath = values.length > 0 ? values[0] : "";
        }

        return normalizePath(classPath + methodPath);
    }

    private String resolveHttpMethod(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        if (method.isAnnotationPresent(GetMapping.class)) {
            return "GET";
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            return "POST";
        }
        if (method.isAnnotationPresent(PutMapping.class)) {
            return "PUT";
        }
        if (method.isAnnotationPresent(PatchMapping.class)) {
            return "PATCH";
        }
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            return "DELETE";
        }
        return "REQUEST";
    }

    private String humanizeMethodName(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            return "API";
        }
        String withSpaces = methodName.replaceAll("([a-z])([A-Z])", "$1 $2");
        return withSpaces.substring(0, 1).toUpperCase() + withSpaces.substring(1);
    }

    private boolean isPublicApiPath(String path) {
        String normalizedPath = normalizePath(path);
        for (String pattern : PUBLIC_PATH_PATTERNS) {
            if (matchPathPattern(normalizedPath, pattern)) {
                return true;
            }
        }
        return false;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.replaceAll("//+", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean matchPathPattern(String path, String pattern) {
        String regex =
                "^"
                        + pattern.replace(".", "\\.")
                                .replace("**", ".*")
                                .replaceAll("\\{[^/]+\\}", "[^/]+")
                        + "$";
        return path.matches(regex);
    }

    private List<String> parseAuthorityTokens(
            String expression, Pattern singlePattern, Pattern multiPattern) {
        Set<String> values = new LinkedHashSet<>();

        Matcher singleMatcher = singlePattern.matcher(expression);
        while (singleMatcher.find()) {
            values.add(singleMatcher.group(1));
        }

        Matcher multiMatcher = multiPattern.matcher(expression);
        while (multiMatcher.find()) {
            String group = multiMatcher.group(1);
            Matcher quotedMatcher = QUOTED_TOKEN_PATTERN.matcher(group);
            while (quotedMatcher.find()) {
                values.add(quotedMatcher.group(1));
            }
        }

        return new ArrayList<>(values);
    }

    private String buildEnumSectionForCurrentApi(HandlerMethod handlerMethod) {
        Set<Class<? extends Enum<?>>> enumTypes = new LinkedHashSet<>();

        for (var parameter : handlerMethod.getMethod().getGenericParameterTypes()) {
            collectEnumsShallow(parameter, enumTypes, 2);
        }
        collectEnumsShallow(handlerMethod.getMethod().getGenericReturnType(), enumTypes, 2);

        if (enumTypes.isEmpty()) {
            return "- 없음";
        }

        List<Class<? extends Enum<?>>> sorted =
                enumTypes.stream().sorted(Comparator.comparing(Class::getSimpleName)).toList();
        List<String> lines = new ArrayList<>();
        for (Class<? extends Enum<?>> enumType : sorted) {
            lines.add("- **" + enumType.getSimpleName() + "**");
            for (String value : enumValueLines(enumType)) {
                lines.add("  - " + value);
            }
        }

        return String.join("\n", lines);
    }

    @SuppressWarnings("unchecked")
    private void collectEnumsShallow(Type type, Set<Class<? extends Enum<?>>> sink, int depth) {
        if (type == null || depth < 0) {
            return;
        }

        if (type instanceof Class<?> clazz) {
            if (clazz.isEnum()) {
                sink.add((Class<? extends Enum<?>>) clazz);
                return;
            }

            if (isLeafClass(clazz)) {
                return;
            }

            if (depth == 0) {
                return;
            }

            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                collectEnumsShallow(field.getGenericType(), sink, depth - 1);
            }
            return;
        }

        if (type instanceof ParameterizedType parameterizedType) {
            collectEnumsShallow(parameterizedType.getRawType(), sink, depth);
            for (Type argType : parameterizedType.getActualTypeArguments()) {
                collectEnumsShallow(argType, sink, depth - 1);
            }
        }
    }

    private boolean isLeafClass(Class<?> clazz) {
        if (clazz.isEnum()) {
            return true;
        }
        Package pkg = clazz.getPackage();
        String packageName = pkg == null ? "" : pkg.getName();
        return clazz.isPrimitive()
                || packageName.startsWith("java.")
                || packageName.startsWith("jakarta.")
                || packageName.startsWith("javax.")
                || packageName.startsWith("org.springframework.");
    }

    private List<String> enumValueLines(Class<? extends Enum<?>> enumType) {
        List<String> values = new ArrayList<>();
        for (Enum<?> constant : enumType.getEnumConstants()) {
            String code = constant.name();
            String label = extractEnumLabel(constant);
            values.add(label == null ? code : code + " (" + label + ")");
        }
        return values;
    }

    private String extractEnumLabel(Enum<?> constant) {
        try {
            Method getDescription = constant.getClass().getMethod("getDescription");
            Object value = getDescription.invoke(constant);
            return Objects.toString(value, null);
        } catch (Exception ignored) {
            return null;
        }
    }
}
