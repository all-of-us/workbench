package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomCBCriteriaDao {
  Page<DbCriteria> findCriteriaByDomainAndStandardAndNameEndsWith(
      String domain, Boolean standard, List<String> endsWithList, Pageable page);

  Page<DbCriteria> findCriteriaByDomainAndStandardAndTermAndNameEndsWith(
      String domain, Boolean standard, String term, List<String> endsWithList, Pageable page);
}
