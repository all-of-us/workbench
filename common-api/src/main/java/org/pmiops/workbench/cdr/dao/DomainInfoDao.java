package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DomainInfo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface DomainInfoDao extends CrudRepository<DomainInfo, Long> {

  // TODO: consider not using @Query since it doesn't let us re-use shared SQL expressions for these
  // methods?

  /**
   * Returns domain metadata and concept counts for domains, matching only standard concepts by
   * name, code, or concept ID. standardConceptCount is populated; allConceptCount and
   * participantCount are not needed and set to zero.
   *
   * @param matchExpression a boolean full text match expression based on the user's query; see
   *     https://dev.mysql.com/doc/refman/5.7/en/fulltext-boolean.html
   */
  @Query(
      value =
          "select new org.pmiops.workbench.cdr.model.DomainInfo(\n"
              + "d.domain, d.domainId, d.name, d.description,\n"
              + "d.conceptId, 0L, COUNT(*), d.participantCount)\n"
              + "from DomainInfo d\n"
              + "join Concept c ON d.domainId = c.domainId\n"
              + "where (c.countValue > 0 or c.sourceCountValue > 0) \n"
              + "and matchConcept(c.conceptName, c.conceptCode, c.vocabularyId, c.synonymsStr, ?1) > 0 and\n"
              + "c.standardConcept IN ('S', 'C')\n"
              + "group by d.domain, d.domainId, d.name, d.description, d.conceptId\n"
              + "order by d.domainId")
  List<DomainInfo> findStandardConceptCounts(String matchExpression);

  /**
   * Returns domain metadata and concept counts for domains, matching both standard and non-standard
   * concepts by name, code, or concept ID. allConceptCount is populated; standardConceptCount and
   * participantCount are not needed and set to zero.
   *
   * @param matchExpression a boolean full text match expression based on the user's query; see
   *     https://dev.mysql.com/doc/refman/5.7/en/fulltext-boolean.html
   */
  @Query(
      value =
          "select new org.pmiops.workbench.cdr.model.DomainInfo(\n"
              + "d.domain, d.domainId, d.name, d.description,\n"
              + "d.conceptId, COUNT(*), 0L, d.participantCount)\n"
              + "from DomainInfo d\n"
              + "join Concept c ON d.domainId = c.domainId\n"
              + "where (c.countValue > 0 or c.sourceCountValue > 0) \n"
              + "and matchConcept(c.conceptName, c.conceptCode, c.vocabularyId, c.synonymsStr, ?1) > 0\n"
              + "group by d.domain, d.domainId, d.name, d.description, d.conceptId\n"
              + "order by d.domainId")
  List<DomainInfo> findAllMatchConceptCounts(String matchExpression);

  List<DomainInfo> findByOrderByDomainId();
}
