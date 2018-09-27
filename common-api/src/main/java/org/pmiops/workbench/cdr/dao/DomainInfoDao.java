package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DomainInfo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface DomainInfoDao extends CrudRepository<DomainInfo, Long> {

  // TODO: consider not using @Query since it doesn't let us re-use shared SQL expressions for these methods?

  /**
   * Returns domain metadata and concept counts for domains, matching standard concepts by name
   * and all concepts by code or concept ID. standardConceptCount is populated; allConceptCount
   * and participantCount are not needed and set to zero.
   * @param matchExpression a boolean full text match expression based on the user's query; see
   *                https://dev.mysql.com/doc/refman/5.7/en/fulltext-boolean.html
   * @param query the exact query that the user entered
   * @param conceptId the converted ID value for the query, or null
   */
  @Query(value="select new org.pmiops.workbench.cdr.model.DomainInfo(\n" +
      "d.domain, d.domainId, d.name, d.description,\n" +
      "d.conceptId, 0L, COUNT(DISTINCT c.conceptId), 0L)\n" +
      "from DomainInfo d\n" +
      "join Concept c ON d.domainId = c.domainId\n" +
      "left join c.synonyms cs\n" +
      "where (c.countValue > 0 or c.sourceCountValue > 0) \n" +
      "and  (((match(c.conceptName, ?1) > 0 or\n" +
      "match(cs.conceptSynonymName, ?1) > 0) and\n" +
      "c.standardConcept IN ('S', 'C')) or (c.conceptId = ?3 or c.conceptCode = ?2))\n" +
      "group by d.domain, d.domainId, d.name, d.description, d.conceptId\n" +
      "order by d.domainId")
  List<DomainInfo> findStandardOrCodeMatchConceptCounts(String matchExpression, String query, Long conceptId);

  /**
   * Returns domain metadata and concept counts for domains, matching only standard concepts by name,
   * code, or concept ID. allConceptCount is populated; standardConceptCount
   * and participantCount are not needed and set to zero.
   * @param matchExpression a boolean full text match expression based on the user's query; see
   *                https://dev.mysql.com/doc/refman/5.7/en/fulltext-boolean.html
   * @param query the exact query that the user entered
   * @param conceptId the converted ID value for the query, or null
   */
  @Query(value="select new org.pmiops.workbench.cdr.model.DomainInfo(\n" +
      "d.domain, d.domainId, d.name, d.description,\n" +
      "d.conceptId, COUNT(DISTINCT c.conceptId), 0L, 0L)\n" +
      "from DomainInfo d\n" +
      "join Concept c ON d.domainId = c.domainId\n" +
      "left join c.synonyms cs\n" +
      "where (c.countValue > 0 or c.sourceCountValue > 0) \n" +
      "and (match(c.conceptName, ?1) > 0 or\n" +
      "match(cs.conceptSynonymName, ?1) > 0 or\n" +
      "c.conceptId = ?3 or c.conceptCode = ?2) and\n" +
      "c.standardConcept IN ('S', 'C')\n" +
      "group by d.domain, d.domainId, d.name, d.description, d.conceptId\n" +
      "order by d.domainId")
  List<DomainInfo> findStandardConceptCounts(String matchExpression, String query, Long conceptId);

  /**
   * Returns domain metadata and concept counts for domains, matching both standard and non-standard
   * concepts by name, code, or concept ID. allConceptCount is populated; standardConceptCount
   * and participantCount are not needed and set to zero.
   * @param matchExpression a boolean full text match expression based on the user's query; see
   *                https://dev.mysql.com/doc/refman/5.7/en/fulltext-boolean.html
   * @param query the exact query that the user entered
   * @param conceptId the converted ID value for the query, or null
   */
  @Query(value="select new org.pmiops.workbench.cdr.model.DomainInfo(\n" +
      "d.domain, d.domainId, d.name, d.description,\n" +
      "d.conceptId, COUNT(DISTINCT c.conceptId), 0L, 0L)\n" +
      "from DomainInfo d\n" +
      "join Concept c ON d.domainId = c.domainId\n" +
      "left join c.synonyms cs\n" +
      "where (c.countValue > 0 or c.sourceCountValue > 0) \n" +
      "and (match(c.conceptName, ?1) > 0 or\n" +
      "match(cs.conceptSynonymName, ?1) > 0 or\n" +
      "c.conceptId = ?3 or c.conceptCode = ?2)\n" +
      "group by d.domain, d.domainId, d.name, d.description, d.conceptId\n" +
      "order by d.domainId")
  List<DomainInfo> findAllMatchConceptCounts(String matchExpression, String query, Long conceptId);

  List<DomainInfo> findByOrderByDomainId();
}
