package org.pmiops.workbench.cohortbuilder.util;

import com.google.common.collect.ListMultimap;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.math.NumberUtils;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchGroupItem;

public class ValidationPredicates {

  public static Predicate<Collection> isEmpty() {
    return params -> params.isEmpty();
  }

  public static Predicate<Object> betweenOperator() {
    return t ->
        Operator.BETWEEN.equals(
            (t instanceof Attribute)
                ? ((Attribute) t).getOperator()
                : ((Modifier) t).getOperator());
  }

  public static Predicate<Object> notBetweenAndNotInOperator() {
    return t ->
        !Operator.BETWEEN.equals(
                (t instanceof Attribute)
                    ? ((Attribute) t).getOperator()
                    : ((Modifier) t).getOperator())
            && !Operator.IN.equals(
                (t instanceof Attribute)
                    ? ((Attribute) t).getOperator()
                    : ((Modifier) t).getOperator());
  }

  public static Predicate<Object> operandsNotTwo() {
    return t ->
        ((t instanceof Attribute) ? ((Attribute) t).getOperands() : ((Modifier) t).getOperands())
                .size()
            != 2;
  }

  public static Predicate<Object> operandsEmpty() {
    return t ->
        ((t instanceof Attribute) ? ((Attribute) t).getOperands() : ((Modifier) t).getOperands())
            .isEmpty();
  }

  public static Predicate<Object> operandsNotNumbers() {
    return t ->
        !((t instanceof Attribute) ? ((Attribute) t).getOperands() : ((Modifier) t).getOperands())
            .stream()
                .filter(o -> !NumberUtils.isCreatable(o))
                .collect(Collectors.toList())
                .isEmpty();
  }

  public static Predicate<Object> operandsNotOne() {
    return t ->
        ((t instanceof Attribute) ? ((Attribute) t).getOperands() : ((Modifier) t).getOperands())
                .size()
            != 1;
  }

  public static Predicate<Object> operatorNull() {
    return t ->
        ((t instanceof Attribute) ? ((Attribute) t).getOperator() : ((Modifier) t).getOperator())
            == null;
  }

  public static Predicate<Object> operandsNotDates() {
    return t ->
        !((t instanceof Attribute) ? ((Attribute) t).getOperands() : ((Modifier) t).getOperands())
            .stream()
                .filter(
                    date -> {
                      try {
                        new SimpleDateFormat("yyyy-MM-dd").parse(date);
                        return false;
                      } catch (ParseException pe) {
                        return true;
                      }
                    })
                .collect(Collectors.toList())
                .isEmpty();
  }

  public static Predicate<SearchGroupItem> temporalGroupNull() {
    return sgi -> sgi.getTemporalGroup() == null;
  }

  public static Predicate<ListMultimap<Integer, SearchGroupItem>> notZeroAndNotOne() {
    return itemMap -> !itemMap.keySet().containsAll(Arrays.asList(0, 1));
  }
}
