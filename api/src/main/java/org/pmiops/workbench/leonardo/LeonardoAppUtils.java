package org.pmiops.workbench.leonardo;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.utils.Matchers;

public class LeonardoAppUtils {
  private static final Pattern APP_NAME_PATTERN =
      Pattern.compile("all-of-us-(?<userid>\\d+)-(?<appType>[a-zA-Z]*)");

  private static final String APP_TYPE_GROUP_NAME = "appType";

  private static final Map<AppType, String> APP_DISPLAY_NAMES =
      Map.of(AppType.CROMWELL, "Cromwell", AppType.RSTUDIO, "RStudio", AppType.SAS, "SAS");

  public static Optional<AppType> appServiceNameToAppType(String gkeServiceName) {
    return Matchers.getGroup(APP_NAME_PATTERN, gkeServiceName, APP_TYPE_GROUP_NAME)
        .map(s -> AppType.valueOf(s.toUpperCase()));
  }

  public static String appDisplayName(AppType appType) {
    return APP_DISPLAY_NAMES.get(appType);
  }
}
