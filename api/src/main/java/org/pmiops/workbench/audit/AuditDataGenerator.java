package org.pmiops.workbench.audit;

import com.google.cloud.logging.LogEntry;
import com.google.common.collect.ImmutableList;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AuditDataGenerator {
  private static final SecureRandom random = new SecureRandom();
  private static final List<String> TARGET_PROPERTIES =
      ImmutableList.of("HEIGHT", "WIDTH", "VOLUME", "LUMINOSITY", "REFRACTION",
          "OWNER", "RENTER");
  private static final List<String> TARGET_PROPERTY_VALUES = ImmutableList.of("3", "red", "\t\uD83C\uDFA9\n",
      "blue", "leavy", "heavy", "105", "867-5309", "lorem", "ipsum");
  /**
   * Generate a large number of random LogEntries
   * @param numRows
   */
  public static List<LogEntry> generateRandomLogEntries(int numRows) {
    // N times, generate a random entry
    return Collections.emptyList();
  }

  private static LogEntry generateEntry() {
    final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    final AgentType agentType = randomEnum(AgentType.class);
    final long agentId = random.nextInt(100); // agents are 0-99
    final String agentEmail = "wgates@msn.com";
    final TargetType targetType = randomEnum(TargetType.class);
    final long targetId = random.nextInt(100) + 100; // targets are 100-199
    final String targetProperty = randomFromList(TARGET_PROPERTIES);
    final String previousValue = randomFromList(TARGET_PROPERTY_VALUES);

  }

  public static boolean includeOptionalColumn() {
    return random.nextGaussian() < 0.1;
  }

  public static <T extends Enum<?>> T randomEnum(Class<T> enumClass) {
    T[] values = enumClass.getEnumConstants();
    int index = random.nextInt(values.length);
    return values[index];
  }

  public static <T> T randomFromList(List<T> values) {
    int index = random.nextInt(values.size());
    return values.get(index);
  }

//  public static String randomStringFromList(List<String> values) {
//    int index = random.nextInt(values.size());
//    return values.get(index);
//  }

}
