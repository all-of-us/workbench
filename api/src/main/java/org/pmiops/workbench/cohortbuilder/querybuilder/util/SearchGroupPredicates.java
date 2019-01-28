package org.pmiops.workbench.cohortbuilder.querybuilder.util;

import com.google.common.collect.ListMultimap;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.model.TemporalTime;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchGroupPredicates {

  private static final List<String> MENTION_TYPES =
    Stream.of(TemporalMention.values())
      .map(Enum::name)
      .collect(Collectors.toList());

  private static final List<String> TIME_TYPES =
    Stream.of(TemporalTime.values())
      .map(Enum::name)
      .collect(Collectors.toList());

  private static final List<String> REQUIRED_TIME_VALUE_TYPES =
    TIME_TYPES.stream().skip(1).collect(Collectors.toList());

  public static Predicate<SearchGroupItem> temporalGroupNull() {
    return sgi -> sgi.getTemporalGroup() == null;
  }

  public static Predicate<SearchGroupItem> temporalGroupNotZeroAndNotOne() {
    return sgi -> sgi.getTemporalGroup() != 0 && sgi.getTemporalGroup() != 1;
  }

  public static Predicate<ListMultimap<Integer, SearchGroupItem>> notContainsTwoGroups() {
    return itemMap -> itemMap.keySet().size() != 2;
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
