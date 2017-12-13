package org.pmiops.workbench.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Base64;
import org.pmiops.workbench.exceptions.BadRequestException;

public final class PaginationToken {

  private static final Charset UTF_8 = Charset.forName("UTF-8");

  @JsonProperty("o")
  private long offset;
  @JsonProperty("h")
  private int parameterHash;

  public PaginationToken(long offset, int parameterHash) {
    this.offset = offset;
    this.parameterHash = parameterHash;
  }

  public String toBase64() {
    return Base64.getEncoder().encodeToString(new Gson().toJson(this).getBytes(UTF_8));
  }

  public long getOffset() {
    return offset;
  }

  public int getParameterHash() {
    return parameterHash;
  }

  public boolean matchesParameters(Object... parameters) {
    return Objects.hash(parameters) == parameterHash;
  }

  public static PaginationToken of(long offset, Object... parameters) {
    return new PaginationToken(offset, Objects.hash(parameters));
  }

  public static PaginationToken fromBase64(String str) {
    String json = new String(Base64.getDecoder().decode(str), UTF_8);
    try {
      PaginationToken result = new Gson().fromJson(json, PaginationToken.class);
      if (result.getOffset() < 0) {
        throw new BadRequestException(String.format("Invalid pagination offset: %d",
            result.getOffset()));
      }
      return result;
    } catch (JsonSyntaxException e) {
      throw new BadRequestException(String.format("Invalid pagination token: %s", str));
    }
  }
}
