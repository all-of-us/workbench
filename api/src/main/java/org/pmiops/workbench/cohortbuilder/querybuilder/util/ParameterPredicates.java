package org.pmiops.workbench.cohortbuilder.querybuilder.util;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.model.AttrName;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TreeSubType;
import org.pmiops.workbench.model.TreeType;

public class ParameterPredicates {

  private static final int SYSTOLIC = 903118;
  private static final int DIASTOLIC = 903115;

  private static final List<String> ICD_TYPES =
      Arrays.asList(TreeType.ICD9.toString(), TreeType.ICD10.toString());

  private static final List<String> CODE_TYPES =
      Arrays.asList(
          TreeType.ICD9.toString(),
          TreeType.ICD10.toString(),
          TreeType.CPT.toString(),
          TreeType.SNOMED.toString());

  private static final List<String> CODE_SUBTYPES =
      Arrays.asList(
          TreeSubType.CM.toString(),
          TreeSubType.PROC.toString(),
          TreeSubType.CM.toString(),
          TreeSubType.PCS.toString(),
          TreeSubType.CPT4.toString());

  private static final List<String> DEMO_SUBTYPES =
      Arrays.asList(
          TreeSubType.AGE.toString(),
          TreeSubType.DEC.toString(),
          TreeSubType.GEN.toString(),
          TreeSubType.RACE.toString(),
          TreeSubType.ETH.toString());

  private static final List<String> PM_SUBTYPES =
      Arrays.asList(
          TreeSubType.BP.toString(),
          TreeSubType.HR.toString(),
          TreeSubType.HR_DETAIL.toString(),
          TreeSubType.HEIGHT.toString(),
          TreeSubType.WEIGHT.toString(),
          TreeSubType.BMI.toString(),
          TreeSubType.WC.toString(),
          TreeSubType.HC.toString(),
          TreeSubType.PREG.toString(),
          TreeSubType.WHEEL.toString(),
          TreeSubType.HR_IRR.toString(),
          TreeSubType.HR_NOIRR.toString());

  private static final List<String> DEMO_GEN_RACE_ETH_SUBTYPES =
      Arrays.asList(
          TreeSubType.GEN.toString(), TreeSubType.RACE.toString(), TreeSubType.ETH.toString());

  public static Predicate<List<SearchParameter>> parametersEmpty() {
    return params -> params.isEmpty();
  }

  public static Predicate<List<SearchParameter>> containsAgeAndDec() {
    return params ->
        params.stream()
                .filter(
                    param ->
                        TreeSubType.AGE.toString().equalsIgnoreCase(param.getSubtype())
                            || TreeSubType.DEC.toString().equalsIgnoreCase(param.getSubtype()))
                .collect(Collectors.toList())
                .size()
            == 2;
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

  public static Predicate<SearchParameter> typeBlank() {
    return sp -> StringUtils.isBlank(sp.getType());
  }

  public static Predicate<SearchParameter> domainBlank() {
    return sp -> StringUtils.isBlank(sp.getDomain());
  }

  public static Predicate<SearchParameter> groupNull() {
    return sp -> sp.getGroup() == null;
  }

  public static Predicate<SearchParameter> ancestorDataNull() {
    return sp -> sp.getAncestorData() == null;
  }

  public static Predicate<SearchParameter> codeTypeInvalid() {
    return sp -> !CODE_TYPES.stream().anyMatch(sp.getType()::equalsIgnoreCase);
  }

  public static Predicate<SearchParameter> demoTypeInvalid() {
    return sp -> !TreeType.DEMO.toString().equalsIgnoreCase(sp.getType());
  }

  public static Predicate<SearchParameter> pmTypeInvalid() {
    return sp -> !TreeType.PM.toString().equalsIgnoreCase(sp.getType());
  }

  public static Predicate<SearchParameter> drugTypeInvalid() {
    return sp -> !TreeType.DRUG.toString().equalsIgnoreCase(sp.getType());
  }

  public static Predicate<SearchParameter> measTypeInvalid() {
    return sp -> !TreeType.MEAS.toString().equalsIgnoreCase(sp.getType());
  }

  public static Predicate<SearchParameter> visitTypeInvalid() {
    return sp -> !TreeType.VISIT.toString().equalsIgnoreCase(sp.getType());
  }

  public static Predicate<SearchParameter> ppiTypeInvalid() {
    return sp -> !TreeType.PPI.toString().equalsIgnoreCase(sp.getType());
  }

  public static Predicate<SearchParameter> typeICD() {
    return sp -> ICD_TYPES.stream().anyMatch(sp.getType()::equalsIgnoreCase);
  }

  public static Predicate<SearchParameter> subtypeBlank() {
    return sp -> StringUtils.isBlank(sp.getSubtype());
  }

  public static Predicate<SearchParameter> codeSubtypeInvalid() {
    return sp -> !CODE_SUBTYPES.stream().anyMatch(sp.getSubtype()::equalsIgnoreCase);
  }

  public static Predicate<SearchParameter> demoSubtypeInvalid() {
    return sp -> !DEMO_SUBTYPES.stream().anyMatch(sp.getSubtype()::equalsIgnoreCase);
  }

  public static Predicate<SearchParameter> pmSubtypeInvalid() {
    return sp -> !PM_SUBTYPES.stream().anyMatch(sp.getSubtype()::equalsIgnoreCase);
  }

  public static Predicate<SearchParameter> subTypeGenRaceEth() {
    return sp -> DEMO_GEN_RACE_ETH_SUBTYPES.stream().anyMatch(sp.getSubtype()::equalsIgnoreCase);
  }

  public static Predicate<SearchParameter> notTwoAttributes() {
    return sp -> sp.getAttributes().size() != 2;
  }

  public static Predicate<SearchParameter> notNumAttr() {
    return sp ->
        sp.getAttributes().stream()
                .filter(a -> AttrName.NUM.equals(a.getName()))
                .collect(Collectors.toList())
                .size()
            == 0;
  }

  public static Predicate<SearchParameter> notSystolicAndDiastolic() {
    return sp ->
        sp.getAttributes().stream()
                .filter(a -> SYSTOLIC != a.getConceptId() && DIASTOLIC != a.getConceptId())
                .collect(Collectors.toList())
                .size()
            != 0;
  }
}
