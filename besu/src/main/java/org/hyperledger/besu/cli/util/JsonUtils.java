package org.hyperledger.besu.cli.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.File;
import java.io.IOException;

public class JsonUtils {
  public static String readJsonExcludingField(final File genesisFile, final String excludedFieldName) {
    StringBuilder jsonBuilder = new StringBuilder();
    JsonFactory jsonFactory = new JsonFactory();
    try (JsonParser parser = jsonFactory.createParser(genesisFile)) {
      JsonToken token;
      while ((token = parser.nextToken()) != null) {
        if (token == JsonToken.START_OBJECT) {
          jsonBuilder.append(handleObject(parser, excludedFieldName, true));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected error while reading genesis file: " + e.getMessage(), e);
    }
    return jsonBuilder.toString();
  }

  private static String handleObject(final JsonParser parser, final String excludedFieldName, final boolean isRootObject) throws IOException {
    StringBuilder objectBuilder = new StringBuilder();
    if (!isRootObject) objectBuilder.append("{");
    String fieldName;
    boolean isFirstField = true;
    while (parser.nextToken() != JsonToken.END_OBJECT) {
      fieldName = parser.getCurrentName();
      if (fieldName != null && fieldName.equals(excludedFieldName)) {
        parser.skipChildren(); // Skip this field
        continue;
      }
      if (!isFirstField) objectBuilder.append(", ");
      parser.nextToken(); // move to value
      objectBuilder.append('"').append(fieldName).append("\":").append(handleValue(parser, excludedFieldName));
      isFirstField = false;
    }
    if (!isRootObject) objectBuilder.append("}");
    return objectBuilder.toString();
  }

  private static String handleValue(final JsonParser parser, final String excludedFieldName) throws IOException {
    JsonToken token = parser.getCurrentToken();
    switch (token) {
      case START_OBJECT:
        return handleObject(parser, excludedFieldName, false);
      case START_ARRAY:
        return handleArray(parser, excludedFieldName);
      case VALUE_STRING:
        return "\"" + parser.getText() + "\"";
      case VALUE_NUMBER_INT:
      case VALUE_NUMBER_FLOAT:
        return parser.getNumberValue().toString();
      case VALUE_TRUE:
      case VALUE_FALSE:
        return parser.getBooleanValue() ? "true" : "false";
      case VALUE_NULL:
        return "null";
      default:
        throw new IllegalStateException("Unrecognized token: " + token);
    }
  }

  private static String handleArray(final JsonParser parser, final String excludedFieldName) throws IOException {
    StringBuilder arrayBuilder = new StringBuilder();
    arrayBuilder.append("[");
    boolean isFirstElement = true;
    while (parser.nextToken() != JsonToken.END_ARRAY) {
      if (!isFirstElement) arrayBuilder.append(", ");
      arrayBuilder.append(handleValue(parser, excludedFieldName));
      isFirstElement = false;
    }
    arrayBuilder.append("]");
    return arrayBuilder.toString();
  }
}
