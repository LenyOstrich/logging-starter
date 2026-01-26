package ru.iukr.loggingstarter.webfilter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import ru.iukr.loggingstarter.filter.LoggingEndpointFilter;
import ru.iukr.loggingstarter.masker.LoggingMasker;

import java.lang.reflect.Type;
import java.util.Optional;

@ControllerAdvice
@RequiredArgsConstructor
public class WebLoggingRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private static final Logger log = LoggerFactory.getLogger(WebLoggingRequestBodyAdvice.class);

    private final LoggingMasker loggingMasker;
    private final LoggingEndpointFilter loggingEndpointFilter;

    @Autowired
    private HttpServletRequest request;

    @Override
    public Object afterBodyRead(Object body,
                                HttpInputMessage inputMessage,
                                MethodParameter parameter,
                                Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        String requestURI = request.getRequestURI() + formatQueryString(request);

        boolean ignore = loggingEndpointFilter.isIgnoredEndpoint(requestURI);
        if (!ignore) {
            log.info("Тело запроса: {}", loggingMasker.maskFields(body));
        }
        return super.afterBodyRead(body, inputMessage, parameter, targetType, converterType);
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    private String formatQueryString(HttpServletRequest request) {
        return Optional.ofNullable(request.getQueryString())
                .map(qs -> "?" + qs)
                .orElse(Strings.EMPTY);
    }
}
