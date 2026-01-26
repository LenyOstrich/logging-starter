package ru.iukr.loggingstarter.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "logging.web-logging")
public class LoggingWebProperties {

    private List<String> maskedHeaders = List.of();
    private String maskHeadersValue = "****";

    private List<String> maskedFields = List.of();
    private String maskFieldsValue = "****";

    private List<String> ignoredEndpoints = List.of();
}
