package org.pmiops.workbench.utils;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DaoUtils {

  /**
   * Helper for mapping queries. For example, to count all workspaces by active status.
   * @param queryResults - Generally dao.findAll()
   * @param indexer - method to return a value. Frequently a method reference like DbUser::getDisabled
   * @param <ENTITY_T> - Type of entity
   * @param <ATTRIBUTE_T> - Type of attribute
   * @return
   */
  public static <ENTITY_T, ATTRIBUTE_T> Map<ATTRIBUTE_T, Long> getAttributeToCountMap(
      Iterable<ENTITY_T> queryResults,
      Function<ENTITY_T, ATTRIBUTE_T> indexer) {
    return ImmutableMap.copyOf(
        StreamSupport.stream(queryResults.spliterator(), false)
            .collect(Collectors.groupingBy(indexer, Collectors.counting())));
  }

}
