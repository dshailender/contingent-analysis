package com.example.contingentanalysis.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    public static final String CORRELATION_ID_HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    private static final Pattern VALID_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            String incomingId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (incomingId == null || incomingId.isBlank()) {
                incomingId = httpRequest.getHeader("X-Correlation-Id");
            }

            String correlationId;
            if (incomingId != null && !incomingId.isBlank() && VALID_ID_PATTERN.matcher(incomingId.trim()).matches()) {
                correlationId = incomingId.trim();
            } else {
                correlationId = UUID.randomUUID().toString();
            }

            MDC.put(MDC_KEY, correlationId);
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

            try {
                chain.doFilter(request, response);
            } finally {
                MDC.remove(MDC_KEY);
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}

