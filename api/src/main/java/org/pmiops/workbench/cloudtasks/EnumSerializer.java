package org.pmiops.workbench.cloudtasks;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

// serializes enum values as their values instead of their enum constants, needed by Task Queues
// https://stackoverflow.com/questions/63059972/make-gson-to-use-enum-string-value-instead-of-constant-name
public class EnumSerializer<T extends Enum<T>> implements JsonSerializer<T> {
  @Override
  public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(src.toString());
  }
}
