package com.daoninhthai.payment.config;

import com.daoninhthai.payment.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String idempotencyKey = request.getHeader("X-Idempotency-Key");

        if (!StringUtils.hasText(idempotencyKey)) {
            return true;
        }

        Optional<Object> cachedResponse = idempotencyService.checkAndGet(idempotencyKey);

        if (cachedResponse.isPresent()) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(objectMapper.writeValueAsString(cachedResponse.get()));
            return false;
        }

        return true;
    }
}
