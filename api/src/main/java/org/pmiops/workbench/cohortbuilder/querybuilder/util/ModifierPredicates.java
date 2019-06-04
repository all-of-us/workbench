package org.pmiops.workbench.cohortbuilder.querybuilder.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.math.NumberUtils;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.Operator;

public class ModifierPredicates {

  public static Predicate<List<Modifier>> notOneModifier() {
    return mods -> mods.size() != 1;
  }

  public static Predicate<Modifier> betweenOperator() {
    return m -> Operator.BETWEEN.equals(m.getOperator());
  }

  public static Predicate<Modifier> operandsNotTwo() {
    return m -> m.getOperands().size() != 2;
  }

  public static Predicate<Modifier> notBetweenOperator() {
    return m -> !Operator.BETWEEN.equals(m.getOperator());
  }

  public static Predicate<Modifier> operandsNotOne() {
    return m -> m.getOperands().size() != 1;
  }

  public static Predicate<Modifier> operandsNotNumbers() {
    return m ->
        !m.getOperands().stream()
            .filter(o -> !NumberUtils.isNumber(o))
            .collect(Collectors.toList())
            .isEmpty();
  }

  public static Predicate<Modifier> operatorNull() {
    return m -> m.getOperator() == null;
  }

  public static Predicate<Modifier> operandsEmpty() {
    return m -> m.getOperands().isEmpty();
  }

  public static Predicate<Modifier> operandsNotDates() {
    return m ->
        !m.getOperands().stream()
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

  public static Predicate<Modifier> operatorNotIn() {
    return m -> !Operator.IN.equals(m.getOperator());
  }
}
