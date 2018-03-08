package org.pmiops.workbench.test;

import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;

public class SearchRequests {

  private static final long MALE_CONCEPT_ID = 8507;
  private static final long FEMALE_CONCEPT_ID = 8532;
  private static final long WEIRD_CONCEPT_ID = 2;
  private static final String DEMO_DOMAIN = "DEMO";
  private static final String GENDER_SUBTYPE = "GEN";
  private static final String DEMO_TYPE = "DEMO";
  public static final String ICD9_TYPE = "ICD9";
  public static final String ICD9_GROUP_CODE = "001";

  private SearchRequests() {
  }

  public static SearchRequest genderRequest(long... conceptIds) {
    SearchGroupItem searchGroupItem = new SearchGroupItem().id("id1").type(DEMO_TYPE);
    for (long conceptId: conceptIds) {
      SearchParameter parameter = new SearchParameter().domain(DEMO_DOMAIN).subtype(GENDER_SUBTYPE)
          .conceptId(conceptId);
      searchGroupItem.addSearchParametersItem(parameter);
    }
    return searchRequest(searchGroupItem);
  }

  public static SearchRequest codesRequest(String type, String... codes) {
    SearchGroupItem searchGroupItem = new SearchGroupItem().id("id1").type(type);
    for (String code: codes) {
      SearchParameter parameter = new SearchParameter().type(type).group(true).value(code);
      searchGroupItem.addSearchParametersItem(parameter);
    }
    return searchRequest(searchGroupItem);
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
    return codesRequest(ICD9_TYPE, ICD9_GROUP_CODE);
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
