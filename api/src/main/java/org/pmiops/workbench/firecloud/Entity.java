package org.pmiops.workbench.firecloud;

import com.google.gson.JsonObject;
import java.util.Objects;

public class Entity {

  public interface EntityTypes {
    public String PARTICIPANT_SET = "participant_set";
  }

  private final String name;
  private final String type;
  private final JsonObject attributes;

  public Entity(String name, String type, JsonObject attributes) {
    this.name = name;
    this.type = type;
    this.attributes = attributes;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public JsonObject getAttributes() {
    return attributes;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, attributes);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Entity)) {
      return false;
    }
    Entity that = (Entity) obj;
    return Objects.equals(this.name, that.name)
        && Objects.equals(this.type, that.type)
        && Objects.equals(this.attributes, that.attributes);
  }
}
