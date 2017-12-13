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

  private SearchRequests() {
  }

  public static SearchRequest genderRequest(long... conceptIds) {

    SearchGroupItem searchGroupItem = new SearchGroupItem();
    searchGroupItem.setId("id1");
    searchGroupItem.setType(DEMO_TYPE);
    for (long conceptId: conceptIds) {
      SearchParameter parameter = new SearchParameter().domain(DEMO_DOMAIN).subtype(GENDER_SUBTYPE)
          .conceptId(conceptId);
      searchGroupItem.addSearchParametersItem(parameter);
    }
    SearchGroup searchGroup = new SearchGroup();
    searchGroup.setId("id2");
    searchGroup.addItemsItem(searchGroupItem);
    SearchRequest request = new SearchRequest();
    request.addIncludesItem(searchGroup);
    return request;
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
