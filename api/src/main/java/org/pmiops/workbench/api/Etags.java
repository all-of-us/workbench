package org.pmiops.workbench.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.pmiops.workbench.exceptions.BadRequestException;

/**
 * Utility class for creating API etags, to prevent versioning issues during
 * read-modify-write cycles for API clients.
 */
public final class Etags {
  private static final String ETAG_FORMAT = "\"%d\"";
  private static final Pattern ETAG_PATTERN = Pattern.compile("^\"(\\d+)\"$");

  private Etags() {}

  public static String fromVersion(int version) {
    return String.format(ETAG_FORMAT, version);
  }

  public static int toVersion(String etag) {
    Matcher m = ETAG_PATTERN.matcher(etag);
    if (!m.matches()) {
      throw new BadRequestException(String.format("Invalid etag provided: %s", etag));
    }
    return Integer.parseInt(m.group(1));
  }
}
