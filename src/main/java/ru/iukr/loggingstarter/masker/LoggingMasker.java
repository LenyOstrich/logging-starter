package ru.iukr.loggingstarter.masker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.blaauwendraad.masker.json.JsonMasker;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iukr.loggingstarter.properties.LoggingWebProperties;

import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Map;
import java.util.stream.Collectors;

public class LoggingMasker {

    private static final Logger log = LoggerFactory.getLogger(LoggingMasker.class);

    private final ObjectMapper objectMapper;
    private final LoggingWebProperties loggingWebProperties;

    private final Set<String> maskedFields;
    private final Set<String> headersToMask;
    private final String maskedValue;

    public LoggingMasker(ObjectMapper objectMapper, LoggingWebProperties loggingWebProperties) {
        this.objectMapper = objectMapper;
        this.loggingWebProperties = loggingWebProperties;
        this.maskedFields = new HashSet<>(loggingWebProperties.getMaskedFields());
        this.headersToMask = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.headersToMask.addAll(loggingWebProperties.getMaskedHeaders());
        this.maskedValue = loggingWebProperties.getMaskFieldsValue();
    }

    public String maskFields(Object body) {
        if (body == null) {
            return "";
        }
        try {
            String jsonString;

            if (body instanceof String str && isJson(str)) {
                jsonString = str;
            } else {
                jsonString = objectMapper.writeValueAsString(body);
            }
            if (maskedFields.isEmpty()) {
                return jsonString;
            }
            JsonMasker jsonMasker = createJsonMasker();
            return jsonMasker.mask(jsonString);
        } catch (JsonProcessingException e) {
            log.warn("Не удалось сериализовать объект типа {} в JSON: {}",
                    body.getClass().getSimpleName(), e.getMessage(), e);
            return body.toString();
        }
    }

    public String maskHeaders(Map<String, String> headersMap) {
        String maskValue = loggingWebProperties.getMaskHeadersValue();
        return headersMap.entrySet().stream()
                .map(entry -> {
                    String headerName = entry.getKey();
                    return headerName + "=" + (headersToMask.contains(headerName)
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
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            objectMapper.readTree(value);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

}
