package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.CBCriteria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CBCriteriaDao extends CrudRepository<CBCriteria, Long> {

  @Query(value = "select cr from CBCriteria cr " +
    "where cr.domainId = :domain " +
    "and cr.standard = :standard " +
    "and match(synonyms, :term) > 0 " +
    "order by cr.count desc")
  List<CBCriteria> findCriteriaByDomainAndSearchTerm(@Param("domain") String domain,
                                                     @Param("standard") Boolean isStandard,
                                                     @Param("term") String term,
                                                     Pageable page);
}
