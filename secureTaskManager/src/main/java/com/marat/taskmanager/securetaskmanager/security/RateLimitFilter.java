package com.marat.taskmanager.securetaskmanager.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marat.taskmanager.securetaskmanager.config.RateLimitProperties;
import com.marat.taskmanager.securetaskmanager.exception.ApiError;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "security.rate-limit",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        return !"POST".equals(request.getMethod())
                || (!LOGIN_PATH.equals(requestUri)
                && !REGISTER_PATH.equals(requestUri));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String clientIp = request.getRemoteAddr();
        String bucketKey = requestUri + ":" + clientIp;

        Bucket bucket = buckets.computeIfAbsent(
                bucketKey,
                ignored -> createBucket(requestUri)
        );

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader(
                "X-RateLimit-Remaining",
                String.valueOf(probe.getRemainingTokens())
        );

        if (!probe.isConsumed()) {
            long retryAfterSeconds = Math.max(
                    1,
                    Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds()
            );

            log.warn(
                    "Rate limit exceeded: endpoint={}, clientIp={}",
                    requestUri,
                    clientIp
            );

            writeRateLimitResponse(
                    request,
                    response,
                    retryAfterSeconds
            );

            return;
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createBucket(String requestUri) {
        RateLimitProperties.EndpointLimit endpointLimit =
                LOGIN_PATH.equals(requestUri)
                        ? properties.getLogin()
                        : properties.getRegister();

        Bandwidth limit = Bandwidth.classic(
                endpointLimit.getCapacity(),
                Refill.greedy(
                        endpointLimit.getRefillTokens(),
                        Duration.ofMinutes(
                                endpointLimit.getRefillMinutes()
                        )
                )
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private void writeRateLimitResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            long retryAfterSeconds
    ) throws IOException {
        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Too Many Requests")
                .message("Too many requests. Please try again later.")
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setHeader("Cache-Control", "no-store");

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}