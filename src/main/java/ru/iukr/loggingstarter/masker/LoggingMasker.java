package ru.iukr.loggingstarter.masker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.blaauwendraad.masker.json.JsonMasker;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import ru.iukr.loggingstarter.properties.LoggingWebProperties;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

public class LoggingMasker {

    private static final Logger log = LoggerFactory.getLogger(LoggingMasker.class);
    private static final String UNSERIALIZABLE_BODY =
            "<UNSERIALIZABLE_BODY>";
    private static final String EMPTY_BODY =
            "<EMPTY_BODY>";

    private final ObjectMapper objectMapper;
    private final LoggingWebProperties loggingWebProperties;
    private final Set<String> maskedFields;
    private final Set<String> headersToMask;
    private final String maskedValue;
    private final JsonMasker jsonMasker;

    public LoggingMasker(ObjectMapper objectMapper, LoggingWebProperties loggingWebProperties) {
        this.objectMapper = objectMapper;
        this.loggingWebProperties = loggingWebProperties;
        this.maskedFields = new HashSet<>(loggingWebProperties.getMaskedFields());
        this.headersToMask = loggingWebProperties.getMaskedHeaders().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(HashSet::new));
        this.maskedValue = loggingWebProperties.getMaskFieldsValue();
        this.jsonMasker = createJsonMasker();
    }

    public String maskJsonString(String body) {
        if (!StringUtils.hasLength(body)) {
            return EMPTY_BODY;
        }

        if (!isJson(body)) {
            return body;
        }

        return maskedFields.isEmpty()
                ? body
                : jsonMasker.mask(body);
    }

    public String maskObject(Object body) {
        if (body == null) {
            return EMPTY_BODY;
        }

        try {
            String jsonString = objectMapper.writeValueAsString(body);

            return jsonMasker.mask(jsonString);
        } catch (JsonProcessingException e) {
            log.warn("Не удалось сериализовать объект типа {} в JSON: {}",
                    body.getClass().getSimpleName(), e.getMessage(), e);

            return UNSERIALIZABLE_BODY;
        }
    }

    public String maskHeaders(Map<String, String> headersMap) {
        String maskValue = loggingWebProperties.getMaskHeadersValue();

        return headersMap.entrySet().stream()
                .map(entry -> {
                    String headerName = entry.getKey();

                    return headerName + "=" + (headersToMask.contains(headerName.toLowerCase())
                            ? maskValue
                            : entry.getValue());
                })
                .collect(Collectors.joining(","));
    }

    private JsonMasker createJsonMasker() {
        if (maskedValue != null) {
            return JsonMasker.getMasker(
                    JsonMaskingConfig.builder()
                            .maskKeys(maskedFields)
                            .maskStringsWith(maskedValue)
                            .maskNumbersWith(maskedValue)
                            .maskBooleansWith(maskedValue)
                            .build()
            );
        }

        return JsonMasker.getMasker(maskedFields);
    }

    private boolean isJson(String value) {
        try {
            objectMapper.readTree(value);

            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

}
