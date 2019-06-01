package org.pmiops.workbench.test;

import java.util.Arrays;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.model.TemporalTime;
import org.pmiops.workbench.model.TreeSubType;
import org.pmiops.workbench.model.TreeType;

public class SearchRequests {

  private static final long MALE_CONCEPT_ID = 8507;
  private static final long FEMALE_CONCEPT_ID = 8532;
  private static final long WEIRD_CONCEPT_ID = 2;
  private static final String DEMO_DOMAIN = "DEMO";
  private static final String GENDER_SUBTYPE = "GEN";
  private static final String DEMO_TYPE = TreeType.DEMO.name();
  public static final String SEARCHGROUP_ITEM_TYPE = TreeType.CONDITION.name();
  public static final String ICD9_TYPE = TreeType.ICD9.name();
  public static final String ICD9_SUBTYPE = TreeSubType.CM.name();
  public static final String ICD9_GROUP_CODE = "001";

  private SearchRequests() {}

  public static SearchRequest genderRequest(long... conceptIds) {
    SearchGroupItem searchGroupItem = new SearchGroupItem().id("id1").type(DEMO_TYPE);
    for (long conceptId : conceptIds) {
      SearchParameter parameter =
          new SearchParameter().type(DEMO_DOMAIN).subtype(GENDER_SUBTYPE).conceptId(conceptId);
      searchGroupItem.addSearchParametersItem(parameter);
    }
    return searchRequest(searchGroupItem);
  }

  public static SearchRequest codesRequest(
      String groupType, String type, String subtype, String... codes) {
    SearchGroupItem searchGroupItem = new SearchGroupItem().id("id1").type(groupType);
    for (String code : codes) {
      SearchParameter parameter =
          new SearchParameter().type(type).subtype(subtype).group(true).value(code);
      searchGroupItem.addSearchParametersItem(parameter);
    }
    return searchRequest(searchGroupItem);
  }

  public static SearchRequest temporalRequest() {
    SearchParameter icd9 =
        new SearchParameter()
            .type(TreeType.ICD9.name())
            .subtype(TreeSubType.CM.name())
            .group(false)
            .conceptId(1L);
    SearchParameter icd10 =
        new SearchParameter()
            .type(TreeType.ICD10.name())
            .subtype(TreeSubType.CM.name())
            .group(false)
            .conceptId(9L);
    SearchParameter snomed =
        new SearchParameter()
            .type(TreeType.SNOMED.name())
            .subtype(TreeSubType.CM.name())
            .group(false)
            .conceptId(4L);

    SearchGroupItem icd9SGI =
        new SearchGroupItem()
            .type(TreeType.CONDITION.name())
            .addSearchParametersItem(icd9)
            .temporalGroup(0);
    SearchGroupItem icd10SGI =
        new SearchGroupItem()
            .type(TreeType.CONDITION.name())
            .addSearchParametersItem(icd10)
            .temporalGroup(1);
    SearchGroupItem snomedSGI =
        new SearchGroupItem()
            .type(TreeType.CONDITION.name())
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
    return new SearchRequest().includes(Arrays.asList(temporalGroup));
  }

  private static SearchRequest searchRequest(SearchGroupItem searchGroupItem) {
    SearchGroup searchGroup = new SearchGroup();
    searchGroup.setId("id2");
    searchGroup.addItemsItem(searchGroupItem);

    SearchRequest request = new SearchRequest();
    request.addIncludesItem(searchGroup);

    return request;
  }

  public static SearchRequest icd9Codes() {
    return codesRequest(SEARCHGROUP_ITEM_TYPE, ICD9_TYPE, ICD9_SUBTYPE, ICD9_GROUP_CODE);
  }

  public static SearchRequest males() {
    return genderRequest(MALE_CONCEPT_ID);
  }

  public static SearchRequest females() {
    return genderRequest(FEMALE_CONCEPT_ID);
  }

  public static SearchRequest maleOrFemale() {
    return genderRequest(MALE_CONCEPT_ID, FEMALE_CONCEPT_ID);
  }

  public static SearchRequest allGenders() {
    return genderRequest(MALE_CONCEPT_ID, FEMALE_CONCEPT_ID, WEIRD_CONCEPT_ID);
  }
}
