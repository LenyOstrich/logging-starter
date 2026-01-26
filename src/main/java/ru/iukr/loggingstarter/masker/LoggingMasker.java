package ru.iukr.loggingstarter.masker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.iukr.loggingstarter.properties.LoggingWebProperties;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LoggingMasker {

    private static final Logger log = LoggerFactory.getLogger(LoggingMasker.class);

    private final ObjectMapper objectMapper;
    private final LoggingWebProperties loggingWebProperties;

    public String maskFields(Object body) {
        List<String> maskedFields = loggingWebProperties.getMaskedFields();
        String maskedValue = loggingWebProperties.getMaskFieldsValue();
        boolean isString = body instanceof String;
        try {
            String jsonBody = isString ? (String) body : objectMapper.writeValueAsString(body);
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode bodyNode = root.get("body");
            if (!maskedFields.isEmpty()) {
                setMaskedValue(bodyNode, maskedFields, maskedValue);
            }
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            log.warn("Не удалось распарсить body как JSON. Маскирование не было выполнено.");
        }
        return isString ? (String) body : body.toString();
    }

    public String maskHeaders(Map<String, String> headersMap) {
        Set<String> headersToMask = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        headersToMask.addAll(loggingWebProperties.getMaskedHeaders());
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

    private void setMaskedValue(JsonNode bodyNode, List<String> maskedFields, String maskedValue) {
        if (bodyNode != null && bodyNode.isObject()) {
            ObjectNode bodyObject = (ObjectNode) bodyNode;
            for (String field : maskedFields) {
                if (bodyObject.has(field)) {
                    bodyObject.put(field, maskedValue);
                }
            }
        }
    }
}
