package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DomainInfo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface DomainInfoDao extends CrudRepository<DomainInfo, Long> {

  // TODO: consider not using @Query since it doesn't let us re-use shared SQL expressions for these methods?

  /**
   * Returns domain metadata and concept counts for domains, matching standard concepts by name
   * and all concepts by code or concept ID.
   * @param matchExpression a boolean full text match expression based on the user's query; see
   *                https://dev.mysql.com/doc/refman/5.7/en/fulltext-boolean.html
   * @param query the exact query that the user entered
   */
  @Query(nativeQuery=true,value="select d.domain, d.domain_id, d.name, d.description,\n" +
      "d.concept_id, COUNT(DISTINCT c.concept_id) as all_concept_count,\n" +
      "SUM(c.standard_concept IN ('S', 'C')) as standard_concept_count,\n" +
      // We don't show participant counts when filtering by keyword, and don't have a way of computing them easily; return 0.
      "0 participant_count\n" +
      "from domain_info d\n" +
      "join concept c on d.domain_id = c.domain_id\n" +
      "left join concept_synonym cs on c.concept_id=cs.concept_id \n" +
      "where (c.count_value > 0 or c.source_count_value > 0) \n" +
      "and  ((((match(c.concept_name) against(?1 in boolean mode) ) or\n" +
      "(match(cs.concept_synonym_name) against(?1 in boolean mode))) and\n" +
      "c.standard_concept IN ('S', 'C')) or (c.concept_id=?2 or c.concept_code=?2))\n" +
      "group by d.domain, d.domain_id, d.name, d.description, d.concept_id\n" +
      "order by d.domainId")
  List<DomainInfo> findStandardOrCodeMatchConceptCounts(String matchExpression, String query);


  /**
   * Returns domain metadata and concept counts for domains, matching both standard and non-standard
   * concepts by name, code, or concept ID.
   * @param matchExpression a boolean full text match expression based on the user's query; see
   *                https://dev.mysql.com/doc/refman/5.7/en/fulltext-boolean.html
   * @param query the exact query that the user entered
   */
  @Query(nativeQuery=true,value="select d.domain, d.domain_id, d.name, d.description,\n" +
      "d.concept_id, COUNT(DISTINCT c.concept_id) as all_concept_count,\n" +
      "SUM(c.standard_concept IN ('S', 'C')) as standard_concept_count,\n" +
      // We don't show participant counts when filtering by keyword, and don't have a way of computing them easily; return 0.
      "0 participant_count\n" +
      "from domain_info d\n" +
      "join concept c on d.domain_id = c.domain_id\n" +
      "left join concept_synonym cs on c.concept_id=cs.concept_id \n" +
      "where (c.count_value > 0 or c.source_count_value > 0) \n" +
      "and  (((match(c.concept_name) against(?1 in boolean mode) ) or\n" +
      "(match(cs.concept_synonym_name) against(?1 in boolean mode))) or c.concept_id=?2 or c.concept_code=?2)\n" +
      "group by d.domain, d.domain_id, d.name, d.description, d.concept_id\n" +
      "order by d.domainId")
  List<DomainInfo> findAllMatchConceptCounts(String matchExpression, String query);

  List<DomainInfo> findByOrderByDomainId();
}
