package org.pmiops.workbench.test;

import java.util.Arrays;
import org.pmiops.workbench.model.CohortDefinition;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.ModifierType;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.model.TemporalTime;

public class CohortDefinitions {

  private static final long MALE_CONCEPT_ID = 8507;
  private static final long FEMALE_CONCEPT_ID = 8532;
  private static final long WEIRD_CONCEPT_ID = 2;

  private CohortDefinitions() {}

  public static CohortDefinition genderRequest(long... conceptIds) {
    SearchGroupItem searchGroupItem =
        new SearchGroupItem().id("id1").type(Domain.PERSON.toString());
    for (long conceptId : conceptIds) {
      SearchParameter parameter =
          new SearchParameter()
              .domain(Domain.PERSON.toString())
              .type(CriteriaType.GENDER.toString())
              .conceptId(conceptId)
              .group(false)
              .standard(true)
              .ancestorData(false);
      searchGroupItem.addSearchParametersItem(parameter);
    }
    return cohortDefinition(searchGroupItem);
  }

  public static CohortDefinition codesRequest(
      String groupType, String type, boolean group, Long conceptId) {
    SearchGroupItem searchGroupItem = new SearchGroupItem().id("id1").type(groupType);
    SearchParameter parameter =
        new SearchParameter()
            .domain(groupType)
            .type(type)
            .group(group)
            .conceptId(conceptId)
            .standard(false)
            .ancestorData(false);
    searchGroupItem.addSearchParametersItem(parameter);
    return cohortDefinition(searchGroupItem);
  }

  public static CohortDefinition modifierRequest(
      String groupType, String type, Modifier... modifiers) {
    SearchGroupItem searchGroupItem = new SearchGroupItem().id("id1").type(groupType);
    SearchParameter parameter =
        new SearchParameter()
            .domain(groupType)
            .type(type)
            .group(true)
            .conceptId(1L)
            .standard(false)
            .ancestorData(false);
    searchGroupItem.addSearchParametersItem(parameter);
    searchGroupItem.setModifiers(Arrays.asList(modifiers));
    return cohortDefinition(searchGroupItem);
  }

  public static CohortDefinition temporalRequest() {
    SearchParameter icd9 =
        new SearchParameter()
            .domain(Domain.CONDITION.toString())
            .type(CriteriaType.ICD9CM.toString())
            .group(false)
            .conceptId(1L)
            .standard(false)
            .ancestorData(false);
    SearchParameter icd10 =
        new SearchParameter()
            .domain(Domain.CONDITION.toString())
            .type(CriteriaType.ICD10CM.toString())
            .group(false)
            .conceptId(9L)
            .standard(false)
            .ancestorData(false);
    SearchParameter snomed =
        new SearchParameter()
            .domain(Domain.CONDITION.toString())
            .type(CriteriaType.SNOMED.name())
            .group(false)
            .conceptId(4L)
            .standard(false)
            .ancestorData(false);

    SearchGroupItem icd9SGI =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .addSearchParametersItem(icd9)
            .temporalGroup(0);
    SearchGroupItem icd10SGI =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .addSearchParametersItem(icd10)
            .temporalGroup(1);
    SearchGroupItem snomedSGI =
        new SearchGroupItem()
            .type(Domain.CONDITION.toString())
            .addSearchParametersItem(snomed)
            .temporalGroup(0);

    // First Mention Of (ICD9Child or Snomed) 5 Days After ICD10
    SearchGroup temporalGroup =
        new SearchGroup()
            .items(Arrays.asList(icd9SGI, snomedSGI, icd10SGI))
            .temporal(true)
            .mention(TemporalMention.FIRST_MENTION)
            .time(TemporalTime.X_DAYS_AFTER)
            .timeValue(5L);
    return new CohortDefinition().includes(Arrays.asList(temporalGroup));
  }

  private static CohortDefinition cohortDefinition(SearchGroupItem searchGroupItem) {
    SearchGroup searchGroup = new SearchGroup();
    searchGroup.setId("id2");
    searchGroup.setTemporal(false);
    searchGroup.addItemsItem(searchGroupItem);

    CohortDefinition cohortDefinition = new CohortDefinition();
    cohortDefinition.addIncludesItem(searchGroup);

    return cohortDefinition;
  }

  public static CohortDefinition icd9Codes() {
    return codesRequest(Domain.CONDITION.toString(), CriteriaType.ICD9CM.toString(), true, 1L);
  }

  public static CohortDefinition icd9CodesChildren() {
    return codesRequest(Domain.CONDITION.toString(), CriteriaType.ICD9CM.toString(), false, 1L);
  }

  public static CohortDefinition conditionPreviewCodes() {
    return codesRequest(
        Domain.CONDITION.toString(), CriteriaType.ICD9CM.toString(), false, 44823922L);
  }

  public static CohortDefinition icd9CodeWithModifiers() {
    return modifierRequest(
        Domain.CONDITION.toString(),
        CriteriaType.ICD9CM.toString(),
        new Modifier()
            .name(ModifierType.AGE_AT_EVENT)
            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
            .operands(Arrays.asList("22")),
        new Modifier()
            .name(ModifierType.ENCOUNTERS)
            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
            .operands(Arrays.asList("1")),
        new Modifier()
            .name(ModifierType.NUM_OF_OCCURRENCES)
            .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
            .operands(Arrays.asList("2")));
  }

  public static CohortDefinition males() {
    return genderRequest(MALE_CONCEPT_ID);
  }

  public static CohortDefinition malesWithEHRData() {
    return genderRequest(MALE_CONCEPT_ID).addDataFiltersItem("HAS_EHR_DATA");
  }

  public static CohortDefinition females() {
    return genderRequest(FEMALE_CONCEPT_ID);
  }

  public static CohortDefinition maleOrFemale() {
    return genderRequest(MALE_CONCEPT_ID, FEMALE_CONCEPT_ID);
  }

  public static CohortDefinition allGenders() {
    return genderRequest(MALE_CONCEPT_ID, FEMALE_CONCEPT_ID, WEIRD_CONCEPT_ID);
  }
}
