package org.pmiops.workbench.cohortbuilder.querybuilder.validation;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.pmiops.workbench.cohortbuilder.querybuilder.MeasurementQueryBuilder;
import org.pmiops.workbench.model.Attribute;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TreeSubType;
import org.pmiops.workbench.model.TreeType;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ParameterPredicates {

  private static final String DECEASED = "Deceased";

  private static final List<String> VALID_DOMAINS =
    Arrays.asList(DomainType.CONDITION.name(), DomainType.PROCEDURE.name(), DomainType.MEASUREMENT.name(),
      DomainType.DRUG.name(), DomainType.OBSERVATION.name(), DomainType.VISIT.name(), DomainType.DEVICE.name());

  private static final List<String> ICD_TYPES =
    Arrays.asList(TreeType.ICD9.name(), TreeType.ICD10.name());

  private static final List<String> CODE_TYPES =
    Arrays.asList(TreeType.ICD9.name(), TreeType.ICD10.name(), TreeType.CPT.name());

  private static final List<String> CODE_SUBTYPES =
    Arrays.asList(TreeSubType.CM.name(), TreeSubType.PROC.name(), TreeSubType.ICD10CM.name(),
      TreeSubType.ICD10PCS.name(), TreeSubType.CPT4.name());

  private static final List<String> DEMO_SUBTYPES =
    Arrays.asList(TreeSubType.AGE.name(), TreeSubType.DEC.name(), TreeSubType.GEN.name(),
      TreeSubType.RACE.name(), TreeSubType.ETH.name());

  public static Predicate<List<SearchParameter>> parametersEmpty() {
    return params -> params.isEmpty();
  }

  public static Predicate<List<SearchParameter>> containsAgeAndDec() {
    return params -> params
      .stream()
      .filter(param -> TreeSubType.AGE.name().equals(param.getSubtype()) ||
        TreeSubType.DEC.name().equals(param.getSubtype()))
      .collect(Collectors.toList()).size() == 2;
  }

  public static Predicate<SearchParameter> attributesEmpty() {
    return sp -> sp.getAttributes().isEmpty();
  }

  public static Predicate<SearchParameter> conceptIdNull() {
    return sp -> sp.getConceptId() == null;
  }

  public static Predicate<SearchParameter> paramChild() {
    return sp -> !sp.getGroup();
  }

  public static Predicate<SearchParameter> paramParent() {
    return sp -> sp.getGroup();
  }

  public static Predicate<SearchParameter> codeBlank() {
    return sp -> StringUtils.isBlank(sp.getValue());
  }

  public static Predicate<SearchParameter> domainInvalid() {
    return sp -> !VALID_DOMAINS.contains(sp.getDomain());
  }

  public static Predicate<SearchParameter> domainBlank() {
    return sp -> StringUtils.isBlank(sp.getDomain());
  }

  public static Predicate<SearchParameter> typeBlank() {
    return sp -> StringUtils.isBlank(sp.getType());
  }

  public static Predicate<SearchParameter> codeTypeInvalid() {
    return sp -> !CODE_TYPES.contains(sp.getType());
  }

  public static Predicate<SearchParameter> demoTypeInvalid() {
    return sp -> !TreeType.DEMO.name().contains(sp.getType());
  }

  public static Predicate<SearchParameter> typeICD() {
    return sp -> ICD_TYPES.contains(sp.getType());
  }

  public static Predicate<SearchParameter> subtypeBlank() {
    return sp -> StringUtils.isBlank(sp.getSubtype());
  }

  public static Predicate<SearchParameter> codeSubtypeInvalid() {
    return sp -> !CODE_SUBTYPES.contains(sp.getSubtype());
  }

  public static Predicate<SearchParameter> demoSubtypeInvalid() {
    return sp -> !DEMO_SUBTYPES.contains(sp.getSubtype());
  }

  public static Predicate<SearchParameter> subtypeAge() {
    return sp -> sp.getSubtype().equals(TreeSubType.AGE.name());
  }

  public static Predicate<SearchParameter> subtypeDec() {
    return sp -> sp.getSubtype().equals(TreeSubType.DEC.name());
  }

  public static Predicate<SearchParameter> valueNotDec() {
    return sp -> !sp.getValue().equals(DECEASED);
  }
}
