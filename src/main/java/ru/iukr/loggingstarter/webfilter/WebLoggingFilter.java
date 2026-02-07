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
import ru.iukr.loggingstarter.util.NonLoggingEndpointChecker;
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
    private final NonLoggingEndpointChecker nonLoggingEndpointChecker;


    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String requestURI = request.getRequestURI();
        boolean ignore = nonLoggingEndpointChecker.isIgnoredEndpoint(requestURI);

        if (!ignore) {
            String method = request.getMethod();
            String formattedRequestURI = requestURI + formatQueryString(request);
            String headers = inlineHeaders(request);

            log.info("Запрос: {}, {}, {}", method, formattedRequestURI, headers);

            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

            try {
                chain.doFilter(request, responseWrapper);
            } finally {
                byte[] responseBytes = responseWrapper.getContentAsByteArray();
                responseWrapper.copyBodyToResponse();
                String responseHeaders =
                        "headers=" + loggingMasker.maskHeaders(
                                extractResponseHeaders(responseWrapper)
                        );
                String responseBody =
                        "body=" + loggingMasker.maskJsonString(
                                new String(responseBytes, StandardCharsets.UTF_8));
                log.info("Ответ: {} {} {} {} {}",
                        method,
                        formattedRequestURI,
                        response.getStatus(),
                        responseHeaders,
                        responseBody);
            }
        } else {
            chain.doFilter(request, response);
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

    private Map<String, String> extractResponseHeaders(HttpServletResponse response) {
        return response.getHeaderNames().stream()
                .collect(Collectors.toMap(
                        headerName -> headerName,
                        headerName -> String.join(",", response.getHeaders(headerName))
                ));
    }
}
