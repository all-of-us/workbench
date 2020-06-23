package org.pmiops.workbench.utils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Static utility class for working with Matcher objects more easily
public class Matchers {
  private Matchers() {}

  // Assuming there's a named group in the pattern, returns an optional string for the match.
  // This avoids some fairly irritating exceptions and branching.
  public static Optional<String> getGroup(Matcher matcher, String groupName) {
    if (matcher.find()) {
      return Optional.ofNullable(matcher.group(groupName));
    } else {
      return Optional.empty();
    }
  }

  public static Optional<String> getGroup(Pattern pattern, String input, String groupName) {
    return getGroup(pattern.matcher(input), groupName);
  }
}
