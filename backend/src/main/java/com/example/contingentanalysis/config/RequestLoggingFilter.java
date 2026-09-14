package com.example.contingentanalysis.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            long startTime = System.currentTimeMillis();
            String uri = httpRequest.getRequestURI();
            String method = httpRequest.getMethod();

            try {
                chain.doFilter(request, response);
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                int status = httpResponse.getStatus();
                if (uri.startsWith("/api") || uri.startsWith("/actuator") || status >= 400) {
                    if (status >= 500) {
                        log.error("HTTP {} {} -> status {} in {} ms", method, uri, status, duration);
                    } else if (status >= 400) {
                        log.warn("HTTP {} {} -> status {} in {} ms", method, uri, status, duration);
                    } else {
                        log.info("HTTP {} {} -> status {} in {} ms", method, uri, status, duration);
                    }
                }
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}

