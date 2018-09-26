package org.pmiops.workbench.cohortbuilder.querybuilder.util;

import com.google.common.collect.ImmutableMap;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.TreeSubType;

import java.util.Arrays;
import java.util.List;

public final class QueryBuilderConstants {

  //Exception messages
  public static final String EMPTY_MESSAGE = "Bad Request: Search {0} are empty.";
  public static final String NOT_VALID_MESSAGE = "Bad Request: {0} {1} \"{2}\" is not valid.";
  public static final String ONE_OPERAND_MESSAGE = "Bad Request: {0} {1} must have one operand when using the {2} operator.";
  public static final String TWO_OPERAND_MESSAGE = "Bad Request: {0} {1} can only have 2 operands when using the {2} operator";
  public static final String OPERANDS_NUMERIC_MESSAGE = "Bad Request: {0} {1} operands must be numeric.";
  public static final String ONE_MODIFIER_MESSAGE = "Bad Request: Please provide one {0} modifier.";
  public static final String DATE_MODIFIER_MESSAGE = "Bad Request: {0} {1} must be a valid date.";
  public static final String NOT_IN_MODIFIER_MESSAGE = "Bad Request: Modifier {0} must provide {1} operator.";
  public static final String AGE_DEC_MESSAGE = "Bad Request: Attribute Age and Deceased cannot be used together.";
  public static final String CATEGORICAL_MESSAGE = "Bad Request: Attribute Categorical must provide In operator.";
  public static final String BP_TWO_ATTRIBUTE_MESSAGE =
    "Bad Request: Attribute Blood Pressure must provide two attributes(systolic and diastolic).";

  //Exception Message parameters
  public static final String PARAMETERS = "Parameters";
  public static final String ATTRIBUTES = "Attributes";
  public static final String PARAMETER = "Search Parameter";
  public static final String ATTRIBUTE = "Attribute";
  public static final String MODIFIER = "Modifier";
  public static final String OPERANDS = "Operands";
  public static final String OPERATOR = "Operator";
  public static final String TYPE = "Type";
  public static final String SUBTYPE = "Subtype";
  public static final String DOMAIN = "Domain";
  public static final String CONCEPT_ID = "Concept Id";
  public static final String CODE = "Code";
  public static final String NAME = "Name";
  public static final String VALUE = "Value";
  public static final String NUMERICAL = "NUM";
  public static final String CATEGORICAL = "CAT";
  public static final String BOTH = "BOTH";
  public static final String LAB = "LAB";

  //Display text for modifiers
  public static ImmutableMap<ModifierType, String> modifierText = ImmutableMap.<ModifierType, String>builder()
    .put(ModifierType.AGE_AT_EVENT, "Age at Event")
    .put(ModifierType.EVENT_DATE, "Event Date")
    .put(ModifierType.NUM_OF_OCCURRENCES, "Number of Occurrences")
    .put(ModifierType.ENCOUNTERS, "Visit Type")
    .build();

  //Display text for Operators
  public static ImmutableMap<Operator, String> operatorText = ImmutableMap.<Operator, String>builder()
    .put(Operator.EQUAL, "Equal")
    .put(Operator.NOT_EQUAL, "Not Equal")
    .put(Operator.LESS_THAN, "Less Than")
    .put(Operator.GREATER_THAN, "Greater Than")
    .put(Operator.LESS_THAN_OR_EQUAL_TO, "Less Than Or Equal To")
    .put(Operator.GREATER_THAN_OR_EQUAL_TO, "Greater Than Or Equal To")
    .put(Operator.LIKE, "Like")
    .put(Operator.IN, "In")
    .put(Operator.BETWEEN, "Between")
    .build();

  //Physical Measurement types that have attributes
  public static final List<String> PM_TYPES_WITH_ATTR =
    Arrays.asList(TreeSubType.BP.name(),
      TreeSubType.HR_DETAIL.toString(),
      TreeSubType.HEIGHT.name(),
      TreeSubType.WEIGHT.name(),
      TreeSubType.BMI.name(),
      TreeSubType.WC.name(),
      TreeSubType.HC.name());

  public static final String AGE_AT_EVENT_PREFIX = "age";
  public static final String EVENT_DATE_PREFIX = "event";
  public static final String OCCURRENCES_PREFIX = "occ";
  public static final String ENCOUNTER_PREFIX = "enc";
  public static final String CONCEPTID_PREFIX = "conceptId";
  public static final String ANY = "ANY";

  private QueryBuilderConstants(){}
}
