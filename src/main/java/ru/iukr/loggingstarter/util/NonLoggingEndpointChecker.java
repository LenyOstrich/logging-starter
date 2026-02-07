package ru.iukr.loggingstarter.util;

import lombok.RequiredArgsConstructor;
import org.springframework.util.AntPathMatcher;
import ru.iukr.loggingstarter.properties.LoggingWebProperties;

import java.util.List;

@RequiredArgsConstructor
public class NonLoggingEndpointChecker {

    private static final AntPathMatcher matcher = new AntPathMatcher();

    private final LoggingWebProperties loggingWebProperties;

    public boolean isIgnoredEndpoint(String requestURI) {
        List<String> ignoredEndpoints = loggingWebProperties.getNonLoggingEndpoints();
        if (ignoredEndpoints == null || ignoredEndpoints.isEmpty()) {
            return false;
        }
        return ignoredEndpoints.stream()
                .anyMatch(ignored -> matcher.match(ignored, requestURI));
    }
}
