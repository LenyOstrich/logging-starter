package ru.iukr.loggingstarter.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.util.AntPathMatcher;
import ru.iukr.loggingstarter.properties.LoggingWebProperties;

import java.util.List;

@RequiredArgsConstructor
public class LoggingEndpointFilter {

    private static final AntPathMatcher matcher = new AntPathMatcher();

    private final LoggingWebProperties loggingWebProperties;

    public boolean isIgnoredEndpoint(String requestURI) {
        List<String> ignoredEndpoints = loggingWebProperties.getIgnoredEndpoints();
        if (ignoredEndpoints == null || ignoredEndpoints.isEmpty()) {
            return false;
        }
        return ignoredEndpoints.stream()
                .anyMatch(ignored -> matcher.match(ignored, requestURI));
    }
}
