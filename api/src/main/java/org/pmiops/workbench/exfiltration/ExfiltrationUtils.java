package org.pmiops.workbench.exfiltration;

import java.util.Optional;
import java.util.regex.Pattern;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.utils.Matchers;

/**Utilities and constants for exfiltration services. */
public class ExfiltrationUtils {

  public static final long THRESHOLD_MB = 150 * 1024 * 1024; // 150MB

  public static final String EGRESS_OBJECT_LENGTHS_SERVICE_QUALIFIER = "objectLengthsEgressService";
  public static final String OBJECT_LENGTHS_JIRA_HANDLER_QUALIFIER = "objectLengthsJiraHandler";

  public static final String EGRESS_SUMOLOGIC_SERVICE_QUALIFIER = "sumologicEgressService";
  public static final String SUMOLOGIC_JIRA_HANDLER_QUALIFIER = "sumologicJiraHandler";

  private static final Pattern VM_PREFIX_PATTERN = Pattern.compile("all-of-us-(?<userid>\\d+)");
  private static final Pattern GKE_APP_PATTERN = Pattern.compile("all-of-us-(?<userid>\\d+)-(?<appType>\\.*)-(.*)");
  private static final String USER_ID_GROUP_NAME = "userid";
  private static final String APP_TYPE_GROUP_NAME = "userid";

  public static Optional<Long> gceVmNameToUserDatabaseId(String vmPrefix) {
    return Matchers.getGroup(VM_PREFIX_PATTERN, vmPrefix, USER_ID_GROUP_NAME).map(Long::parseLong);
  }

  public static Optional<Long> gkeServiceNameToUserDatabaseId(String gkeServiceName) {
    return Matchers.getGroup(VM_PREFIX_PATTERN, gkeServiceName, USER_ID_GROUP_NAME).map(Long::parseLong);
  }

  public static Optional<AppType> extractAppTypeFromGkeServiceName(String gkeServiceName) {
    return Matchers.getGroup(GKE_APP_PATTERN, gkeServiceName, APP_TYPE_GROUP_NAME).map(s -> AppType.valueOf(s.toUpperCase()));
  }
}
