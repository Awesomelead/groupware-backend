package kr.co.awesomelead.groupware_backend.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class HttpRequestDurationLoggingFilter extends OncePerRequestFilter {

    private static final long SLOW_REQUEST_THRESHOLD_MS = 1_000L;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        Throwable failure = null;

        try {
            filterChain.doFilter(request, response);
        } catch (Throwable ex) {
            failure = ex;
            throw ex;
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            logRequest(request, response, durationMs, failure);
        }
    }

    private void logRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            long durationMs,
            Throwable failure) {
        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isBlank()) {
            path += "?" + queryString;
        }

        if (failure != null) {
            log.warn(
                    "HTTP_REQUEST method={} uri={} status={} durationMs={} exception={}",
                    request.getMethod(),
                    path,
                    response.getStatus(),
                    durationMs,
                    failure.getClass().getSimpleName());
            return;
        }

        if (durationMs >= SLOW_REQUEST_THRESHOLD_MS) {
            log.warn(
                    "HTTP_REQUEST method={} uri={} status={} durationMs={}",
                    request.getMethod(),
                    path,
                    response.getStatus(),
                    durationMs);
        } else {
            log.info(
                    "HTTP_REQUEST method={} uri={} status={} durationMs={}",
                    request.getMethod(),
                    path,
                    response.getStatus(),
                    durationMs);
        }
    }
}
