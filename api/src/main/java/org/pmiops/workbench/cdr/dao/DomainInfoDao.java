package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DbDomainInfo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface DomainInfoDao extends CrudRepository<DbDomainInfo, Long> {

  // TODO: consider not using @Query since it doesn't let us re-use shared SQL expressions for these
  // methods?

  /**
   * Returns domain metadata and concept counts for domains, matching only standard concepts by
   * name, code, or concept ID. standardConceptCount is populated; allConceptCount are not needed
   * and set to zero.
   *
   * @param matchExpression a boolean full text match expression based on the user's query; see
   *     https://dev.mysql.com/doc/refman/5.7/en/fulltext-boolean.html
   */
  @Query(
      value =
          "select new DbDomainInfo(\n"
              + "d.domain, d.domainId, d.name, d.description,\n"
              + "d.conceptId, COUNT(*), COUNT(*), d.participantCount)\n"
              + "from DbDomainInfo d\n"
              + "join DbConcept c ON d.domainId = c.domainId\n"
              + "where (c.countValue > 0 or c.sourceCountValue > 0)\n"
              + "and matchConcept(c.conceptName, c.conceptCode, c.vocabularyId, c.synonymsStr, ?1) > 0 "
              + "and c.standardConcept IN (?2)\n"
              + "group by d.domain, d.domainId, d.name, d.description, d.conceptId\n"
              + "order by d.domainId")
  List<DbDomainInfo> findConceptCounts(String matchExpression, List<String> conceptTypes);

  /**
   * Returns domain metadata and concept counts for domains, matching only PM concepts by name,
   * code, or concept ID. allConceptCount is populated; standardConceptCount are not needed and set
   * to zero.
   *
   * @param matchExpression a boolean full text match expression based on the user's query; see
   *     https://dev.mysql.com/doc/refman/5.7/en/fulltext-boolean.html
   */
  @Query(
      value =
          "select new DbDomainInfo(\n"
              + "d.domain, d.domainId, d.name, d.description,\n"
              + "d.conceptId, COUNT(*), COUNT(*), d.participantCount)\n"
              + "from DbDomainInfo d\n"
              + "join DbConcept c ON 'Measurement' = c.domainId\n"
              + "where (c.countValue > 0 or c.sourceCountValue > 0)\n"
              + "and matchConcept(c.conceptName, c.conceptCode, c.vocabularyId, c.synonymsStr, ?1) > 0\n"
              + "and c.vocabularyId = 'PPI'\n"
              + "and d.domain = 10\n"
              + "and c.conceptClassId = 'Clinical Observation'\n"
              + "and c.standardConcept IN (?2)\n"
              + "group by d.domain, d.domainId, d.name, d.description, d.conceptId\n"
              + "order by d.domainId")
  DbDomainInfo findPhysicalMeasurementConceptCounts(
      String matchExpression, List<String> conceptTypes);

  List<DbDomainInfo> findByOrderByDomainId();
}
