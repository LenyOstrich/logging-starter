package ru.iukr.loggingstarter.webfilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.ContentCachingResponseWrapper;
import ru.iukr.loggingstarter.filter.LoggingEndpointFilter;
import ru.iukr.loggingstarter.masker.LoggingMasker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class WebLoggingFilter extends HttpFilter {

    private static final Logger log = LoggerFactory.getLogger(WebLoggingFilter.class);
    private final LoggingMasker loggingMasker;
    private final LoggingEndpointFilter loggingEndpointFilter;


    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String method = request.getMethod();
        String requestURI = request.getRequestURI();
        boolean ignore = loggingEndpointFilter.isIgnoredEndpoint(requestURI);
        String formattedRequestURI = ignore ? Strings.EMPTY : requestURI + formatQueryString(request);

        if (!ignore) {
            String headers = inlineHeaders(request);
            log.info("Запрос: {}, {}, {}", method, formattedRequestURI, headers);
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            chain.doFilter(request, responseWrapper);
            if (!ignore) {
                String responseBody = "body=" + loggingMasker.maskFields(new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8));
                log.info("Ответ: {} {} {} {}", method, formattedRequestURI, response.getStatus(), responseBody);
            }
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }

    private String inlineHeaders(HttpServletRequest request) {
        Map<String, String> headersMap = Collections.list(request.getHeaderNames()).stream()
                .collect(Collectors.toMap(it -> it, request::getHeader));
        String inlineHeaders = loggingMasker.maskHeaders(headersMap);
        return "headers={" + inlineHeaders + "}";
    }

    private static String formatQueryString(HttpServletRequest request) {
        return Optional.ofNullable(request.getQueryString())
                .map(qs -> "?" + qs)
                .orElse(Strings.EMPTY);
    }
}
