package org.pmiops.workbench.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.diff.JsonDiff;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileConfigs {

  private FileConfigs() {}

  public static class ConfigFormatException extends Exception {
    private final JsonNode jsonDiff;

    ConfigFormatException(JsonNode jsonDiff) {
      super("invalid config JSON, diff:\n" + jsonDiff.toPrettyString());
      this.jsonDiff = jsonDiff;
    }

    public JsonNode getJsonDiff() {
      return jsonDiff;
    }
  }

  public static JsonNode loadConfig(String path, Class<?> configClass)
      throws IOException, ConfigFormatException {
    ObjectMapper jackson = new ObjectMapper();
    String rawJson = new String(Files.readAllBytes(Paths.get(path)), Charset.defaultCharset());
    // Strip all lines starting with '//'.
    String strippedJson = rawJson.replaceAll("\\s*//.*", "");
    JsonNode configJson = jackson.readTree(strippedJson);

    // Make sure the config parses to the appropriate configuration format,
    // and has the same representation after being marshalled back to JSON.
    Gson gson = new Gson();
    Object configObj = gson.fromJson(configJson.toString(), configClass);
    String marshalledJson = gson.toJson(configObj, configClass);
    JsonNode marshalledNode = jackson.readTree(marshalledJson);
    JsonNode marshalledDiff = JsonDiff.asJson(configJson, marshalledNode);

    if (marshalledDiff.size() > 0) {
      throw new ConfigFormatException(marshalledDiff);
    }

    return configJson;
  }
}
