package com.huggingsoft.pilot_main.web.configurations;

import com.huggingsoft.pilot_main.shared.RequestContext;
import com.huggingsoft.pilot_main.shared.RequestContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC; // Import MDC
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
// Ensure this filter runs early, before security filters if needed, or after if you need the authenticated principal.
// Adjust the order based on your needs. Higher values run later. Ordered.HIGHEST_PRECEDENCE runs first.
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter { // Ensures execution once per request

    private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-ID"; // Common practice
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final String AUTH_HEADER_MDC_KEY = "authHeaderPresent"; // Log presence, not value for security


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("RequestContextFilter.doFilterInternal");

        long startTime = System.currentTimeMillis();
        // Generate or retrieve Request ID (check incoming header first)
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }

        String authHeader = request.getHeader("Authorization");
        // WARNING: Be extremely careful about logging or storing the actual Authorization header value.
        // It often contains sensitive credentials (Tokens, Basic Auth).
        // Consider storing only its presence or a masked version if necessary for debugging.

        // 1. Populate MDC
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        MDC.put(AUTH_HEADER_MDC_KEY, String.valueOf(authHeader != null && !authHeader.isEmpty()));

        // 2. Populate ThreadLocal Context
        RequestContext context = new RequestContext(requestId, startTime);
        context.setAuthorizationHeader(authHeader); // Store the raw header (handle securely)
        RequestContextHolder.setContext(context);

        log.info("Starting request [{}] {} {}", requestId, request.getMethod(), request.getRequestURI());

        try {
            // Continue processing the request
            filterChain.doFilter(request, response);
        } finally {
            // 3. Cleanup
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Finished request [{}] {} {} in {} ms. Status: {}",
                    requestId, request.getMethod(), request.getRequestURI(), executionTime, response.getStatus());

            // Clear ThreadLocal
            RequestContextHolder.clearContext();
            // Clear MDC - VERY IMPORTANT
            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(AUTH_HEADER_MDC_KEY);
            // Or use MDC.clear(); if you are sure no other MDC context is needed upstream.
        }
    }
}
