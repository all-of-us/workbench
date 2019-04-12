package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.CBCriteria;
import org.pmiops.workbench.cdr.model.Criteria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface CBCriteriaDao extends CrudRepository<CBCriteria, Long> {

  @Query(value = "select cr from CBCriteria cr " +
    "    where cr.domainId = ?1 " +
    "    and cr.standard = ?2 " +
    "    and match(synonyms, ?3) > 0 " +
    "    order by cr.count desc")
  List<CBCriteria> findCriteriaByDomainAndSearchTerm(String domain,
                                                   Boolean isStandard,
                                                   String term,
                                                   Pageable page);
}
