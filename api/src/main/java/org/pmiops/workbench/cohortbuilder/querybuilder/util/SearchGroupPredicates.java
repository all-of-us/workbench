package org.pmiops.workbench.cohortbuilder.querybuilder.util;

import com.google.common.collect.ListMultimap;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.model.TemporalTime;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SearchGroupPredicates {

  private static final List<String> MENTION_TYPES =
    Arrays.asList(TemporalMention.ANY_MENTION.name(),
      TemporalMention.FIRST_MENTION.name(),
      TemporalMention.LAST_MENTION.name());

  private static final List<String> TIME_TYPES =
    Arrays.asList(TemporalTime.DURING_SAME_ENCOUNTER_AS.name(),
      TemporalTime.X_DAYS_BEFORE.name(),
      TemporalTime.X_DAYS_AFTER.name(),
      TemporalTime.WITHIN_X_DAYS_OF.name());

  private static final List<String> REQUIRED_TIME_VALUE_TYPES =
    TIME_TYPES.stream().skip(1).collect(Collectors.toList());

  public static Predicate<SearchGroupItem> temporalGroupNull() {
    return sgi -> sgi.getTemporalGroup() == null;
  }

  public static Predicate<ListMultimap<Integer, SearchGroupItem>> notContainsTwoGroups() {
    return itemMap -> itemMap.size() != 2;
  }

  public static Predicate<SearchGroup> mentionNull() {
    return sg -> sg.getMention() == null;
  }

  public static Predicate<SearchGroup> mentionInvalid() {
    return sg -> !MENTION_TYPES.stream().anyMatch(sg.getMention()::equalsIgnoreCase);
  }

  public static Predicate<SearchGroup> timeNull() {
    return sg -> sg.getTime() == null;
  }

  public static Predicate<SearchGroup> timeInvalid() {
    return sg -> !TIME_TYPES.stream().anyMatch(sg.getTime()::equalsIgnoreCase);
  }

  public static Predicate<SearchGroup> timeValueNull() {
    return sg -> sg.getTimeValue() == null;
  }

  public static Predicate<SearchGroup> timeValueRequired() {
    return sg -> REQUIRED_TIME_VALUE_TYPES.stream().anyMatch(sg.getTime()::equalsIgnoreCase);
  }
}
