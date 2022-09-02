package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DbCardCount;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomCBCriteriaDao {
  Page<DbCriteria> findCriteriaByDomainAndNameEndsWithAndStandardAndNotType(
      String domain, List<String> endsWithList, Boolean standard, String type, Pageable page);

  Page<DbCriteria> findCriteriaByDomainAndNameEndsWithAndTermAndStandardAndNotType(
      String domain,
      String term,
      List<String> endsWithList,
      Boolean standard,
      String type,
      Pageable page);

  List<DbCriteria> findCriteriaByDomainAndTypeAndStandardAndNameEndsWith(
      String domain,
      List<String> types,
      Boolean standard,
      List<Boolean> hierarchies,
      List<String> endsWithList,
      Pageable page);

  List<DbCriteria> findCriteriaByDomainAndTypeAndStandardAndTermAndNameEndsWith(
      String domain,
      List<String> types,
      Boolean standard,
      List<Boolean> hierarchies,
      String term,
      List<String> endsWithTerms,
      Pageable pageRequest);

  List<DbCardCount> findDomainCountsByNameEndsWithAndStandardAndDomains(
      List<String> endsWithList, Boolean standard, List<String> domains);

  List<DbCardCount> findDomainCountsByTermAndNameEndsWithAndStandardAndDomains(
      String term, List<String> endsWithList, Boolean standard, List<String> domains);

  List<DbCardCount> findSurveyCountsByNameEndsWith(List<String> endsWithList);

  List<DbCardCount> findSurveyCountsByTermAndNameEndsWith(String term, List<String> endsWithList);

  Page<DbCriteria> findSurveyQuestionByNameEndsWith(List<String> endsWithList, Pageable page);

  Page<DbCriteria> findSurveyQuestionByTermAndNameEndsWith(
      String term, List<String> endsWithList, Pageable page);

  Page<DbCriteria> findSurveyQuestionByPathAndNameEndsWith(
      Long id, List<String> endsWithList, Pageable page);

  Page<DbCriteria> findSurveyQuestionByPathAndTermAndNameEndsWith(
      Long id, String term, List<String> endsWithList, Pageable page);
}
