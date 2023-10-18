package org.pmiops.workbench.leonardo;

import java.util.Optional;
import java.util.regex.Pattern;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.utils.Matchers;

public class LeonardoAppUtils {
  private static final Pattern APP_NAME_PATTERN =
      Pattern.compile("all-of-us-(?<userid>\\d+)-(?<appType>[a-zA-Z]*)");

  private static final String APP_TYPE_GROUP_NAME = "appType";

  public static Optional<AppType> appServiceNameToAppType(String gkeServiceName) {
    return Matchers.getGroup(APP_NAME_PATTERN, gkeServiceName, APP_TYPE_GROUP_NAME)
        .map(s -> AppType.valueOf(s.toUpperCase()));
  }
}
