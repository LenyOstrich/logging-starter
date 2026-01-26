package ru.iukr.loggingstarter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import ru.iukr.loggingstarter.aspect.LogExecutionAspect;
import ru.iukr.loggingstarter.filter.LoggingEndpointFilter;
import ru.iukr.loggingstarter.masker.LoggingMasker;
import ru.iukr.loggingstarter.properties.LoggingWebProperties;
import ru.iukr.loggingstarter.webfilter.WebLoggingFilter;
import ru.iukr.loggingstarter.webfilter.WebLoggingRequestBodyAdvice;

@AutoConfiguration
@ConditionalOnProperty(prefix = "logging", value = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LoggingWebProperties.class)
public class LoggingStarterAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "logging.web-logging", value = "enabled", havingValue = "true", matchIfMissing = true)
    public LoggingMasker loggingMasker(ObjectMapper objectMapper, LoggingWebProperties properties) {
        return new LoggingMasker(objectMapper, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "logging.web-logging", value = "enabled", havingValue = "true", matchIfMissing = true)
    public LoggingEndpointFilter loggingEndpointFilter(LoggingWebProperties properties) {
        return new LoggingEndpointFilter(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "logging", value = "log-exec-time", havingValue = "true")
    public LogExecutionAspect logExecutionAspect() {
        return new LogExecutionAspect();
    }

    @Bean
    @ConditionalOnBean({LoggingMasker.class, LoggingEndpointFilter.class})
    @ConditionalOnProperty(prefix = "logging.web-logging", value = "enabled", havingValue = "true", matchIfMissing = true)
    public WebLoggingFilter webLoggingFilter(LoggingMasker loggingMasker, LoggingEndpointFilter filter) {
        return new WebLoggingFilter(loggingMasker, filter);
    }

    @Bean
    @ConditionalOnBean(WebLoggingFilter.class)
    @ConditionalOnProperty(prefix = "logging.web-logging", value = "log-body", havingValue = "true")
    public WebLoggingRequestBodyAdvice webLoggingRequestBodyAdvice(LoggingMasker loggingMasker, LoggingEndpointFilter filter) {
        return new WebLoggingRequestBodyAdvice(loggingMasker, filter);
    }
}
