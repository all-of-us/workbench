package org.pmiops.workbench.audit;

import com.google.cloud.logging.LogEntry;
import com.google.common.collect.ImmutableList;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class AuditDataGenerator {
  private static final SecureRandom random = new SecureRandom();
  private static final List<String> USER_EMAIL_ADDRESSES = ImmutableList.of(
      "a@b.co", "x@y.z", "me@there.now", "allyourbase@belongto.us", "genomeo77@dna.biz");
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
    return IntStream.range(0, numRows - 1)
        .mapToObj(unused -> generateEntry())
        .collect(ImmutableList.toImmutableList());
  }

  private static LogEntry generateEntry() {
    final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    final AgentType agentType = randomEnum(AgentType.class);
    final long agentId = random.nextInt(100); // agents are 0-99
    final Optional<String> agentEmail = buildOptionally(() -> randomFromList(USER_EMAIL_ADDRESSES));
    final ActionType actionType = randomEnum(ActionType.class);
    final TargetType targetType = randomEnum(TargetType.class);
    final Optional<Long> targetId = Optional.of((long) random.nextInt(100) + 100); // targets are 100-199
    final Optional<String> targetProperty = buildOptionally(() -> randomFromList(TARGET_PROPERTIES));
    final Optional<String> previousValue = buildOptionally(() -> randomFromList(TARGET_PROPERTY_VALUES));
    final Optional<String> newValue = buildOptionally(() -> randomFromList(TARGET_PROPERTY_VALUES));
    final AbstractAuditableEvent event = new AuditableEvent(
        timestamp,
        agentType,
        agentId,
        agentEmail,
        actionType,
        targetType,
        targetProperty,
        targetId,
        previousValue,
        newValue);
    return event.toLogEntry();
  }

  public static <T> Optional<T> buildOptionally(Supplier<T> valueSupplier) {
    if (includeOptionalColumn()) {
      return Optional.of(valueSupplier.get());
    } else {
      return Optional.empty();
    }
  }

  public static boolean includeOptionalColumn() {
    return Math.abs(random.nextGaussian()) > 0.8;
  }

  private static <T extends Enum<?>> T randomEnum(Class<T> enumClass) {
    T[] values = enumClass.getEnumConstants();
    int index = random.nextInt(values.length);
    return values[index];
  }

  private static <T> T randomFromList(List<T> values) {
    int index = random.nextInt(values.size());
    return values.get(index);
  }
}
