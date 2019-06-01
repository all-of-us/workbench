package org.pmiops.workbench.cohortbuilder.querybuilder.util;

import com.google.common.collect.ListMultimap;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.TemporalTime;

public class SearchGroupPredicates {

  private static final List<String> REQUIRED_TIME_VALUE_TYPES =
      Stream.of(TemporalTime.values()).skip(1).map(Enum::name).collect(Collectors.toList());

  public static Predicate<SearchGroupItem> temporalGroupNull() {
    return sgi -> sgi.getTemporalGroup() == null;
  }

  public static Predicate<ListMultimap<Integer, SearchGroupItem>> notZeroAndNotOne() {
    return itemMap -> !itemMap.keySet().containsAll(Arrays.asList(0, 1));
  }

  public static Predicate<SearchGroup> mentionInvalid() {
    return sg -> sg.getMention() == null;
  }

  public static Predicate<SearchGroup> timeInvalid() {
    return sg -> sg.getTime() == null;
  }

  public static Predicate<SearchGroup> timeValueNull() {
    return sg -> sg.getTimeValue() == null;
  }

  public static Predicate<SearchGroup> timeValueRequired() {
    return sg -> REQUIRED_TIME_VALUE_TYPES.stream().anyMatch(sg.getTime().name()::equalsIgnoreCase);
  }
}
