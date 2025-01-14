package graphql.kickstart.spring.webclient.boot;

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Getter;

public class GraphQLResponse {

  public static final String ERRORS_FIELD = "errors";

  private final JsonNode data;
  @Getter
  private final List<GraphQLError> errors;
  private final ObjectMapper objectMapper;

  GraphQLResponse(String rawResponse, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;

    JsonNode tree = readTree(rawResponse);
    errors = readErrors(tree);
    data = tree.get("data");
  }

  private JsonNode readTree(String rawResponse) {
    try {
      return objectMapper.readTree(rawResponse);
    } catch (JsonProcessingException e) {
      throw new GraphQLClientException("Cannot read response '" + rawResponse + "'", e);
    }
  }

  private List<GraphQLError> readErrors(JsonNode tree) {
    if (tree.hasNonNull(ERRORS_FIELD)) {
      return convertList(tree.get(ERRORS_FIELD), GraphQLError.class);
    }
    return emptyList();
  }

  private <T> List<T> convertList(JsonNode node, Class<T> type) {
    return objectMapper.convertValue(node, constructListType(type));
  }

  private JavaType constructListType(Class<?> type) {
    return objectMapper.getTypeFactory().constructCollectionType(List.class, type);
  }

  public <T> T get(String fieldName, Class<T> type) {
    if (data.hasNonNull(fieldName)) {
      return objectMapper.convertValue(data.get(fieldName), type);
    }
    return null;
  }

  public <T> T get(String fieldName, TypeReference<T> typeRef) {
    if (data.hasNonNull(fieldName)) {
      return objectMapper.convertValue(data.get(fieldName), typeRef);
    }
    return null;
  }

  public <T> T get(String fieldName, JavaType type) {
    if (data.hasNonNull(fieldName)) {
      return objectMapper.convertValue(data.get(fieldName), type);
    }
    return null;
  }

  public <T> T getFirst(Class<T> type) {
    return getFirstDataEntry().map(it -> objectMapper.convertValue(it, type)).orElse(null);
  }

  public <T> T getFirst(TypeReference<T> typeRef) {
    return getFirstDataEntry().map(it -> objectMapper.convertValue(it, typeRef)).orElse(null);
  }

  public <T> T getFirst(JavaType type) {
    return (T) getFirstDataEntry().map(it -> objectMapper.convertValue(it, type)).orElse(null);
  }

  private Optional<JsonNode> getFirstDataEntry() {
    if (!data.isEmpty()) {
      return Optional.ofNullable(data.fields().next().getValue());
    }
    return Optional.empty();
  }

  public <T> List<T> getList(String fieldName, Class<T> type) {
    if (data.hasNonNull(fieldName)) {
      return convertList(data.get(fieldName), type);
    }
    return emptyList();
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> getFirstList(Class<T> type) {
    return getFirstDataEntry()
        .map(it -> convertList(it, type))
        .map(List.class::cast)
        .orElseGet(Collections::emptyList);
  }

  public void validateNoErrors() {
    if (!errors.isEmpty()) {
      throw new GraphQLErrorsException(errors);
    }
  }

}
