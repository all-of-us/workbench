package org.pmiops.workbench.tools;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Parses and formats SQL timestamps when using GSON.
 */
public class TimestampGsonAdapter extends TypeAdapter<Timestamp> {

  private static final SimpleDateFormat TIME_FORMAT =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'");

  @Override
  public void write(JsonWriter out, Timestamp value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      out.value(TIME_FORMAT.format(value));
    }
  }

  @Override
  public Timestamp read(JsonReader in) throws IOException {
    if (in != null) {
      try {
        return new Timestamp(TIME_FORMAT.parse(in.nextString()).getTime());
      } catch (ParseException e) {
        throw new IOException(e);
      }
    } else {
      return null;
    }
  }
}
