package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.DbDomain;
import org.pmiops.workbench.cdr.model.DomainInfo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DomainInfoDao extends CrudRepository<DomainInfo, Long> {

  // TODO: consider not using @Query since it doesn't let us re-use shared SQL expressions for these methods?
  @Query(nativeQuery=true,value="select d.domain, d.name, d.description,\n" +
      "d.concept_id, COUNT(DISTINCT c.concept_id) as all_concept_count,\n" +
      "SUM(c.standard_concept IN ('S', 'C')) as standard_concept_count,\n" +
      // We don't show participant counts when filtering by keyword, and don't have a way of computing them easily; return 0.
      "0 participant_count\n" +
      "from domain_info d\n" +
      "join domain_to_domain_id dd on d.domain = domain_to_domain_id.domain\n" +
      "join concept c on dd.domain_id = c.domain_id\n" +
      "left join concept_synonym cs on c.concept_id=cs.concept_id \n" +
      "where (c.count_value > 0 or c.source_count_value > 0) \n" +
      "and  ((((match(c.concept_name) against(?1 in boolean mode) ) or\n" +
      "(match(cs.concept_synonym_name) against(?1 in boolean mode))) and\n" +
      "c.standard_concept IN ('S', 'C') or (c.concept_id=?2 or c.concept_code=?2))\n" +
      "group by d.domain, d.domain_display, d.domain_desc, d.db_type, d.concept_id\n" +
      "order by d.domain")
  List<DomainInfo> findStandardOrCodeMatchDomainSearchResults(String keyword, String query);

  @Query(nativeQuery=true,value="select d.domain, d.name, d.description,\n" +
      "d.concept_id, COUNT(DISTINCT c.concept_id) as all_concept_count,\n" +
      "SUM(c.standard_concept IN ('S', 'C')) as standard_concept_count,\n" +
      // We don't show participant counts when filtering by keyword, and don't have a way of computing them easily; return 0.
      "0 participant_count\n" +
      "from domain_info d\n" +
      "join domain_to_domain_id dd on d.domain = domain_to_domain_id.domain\n" +
      "join concept c on dd.domain_id = c.domain_id\n" +
      "left join concept_synonym cs on c.concept_id=cs.concept_id \n" +
      "where (c.count_value > 0 or c.source_count_value > 0) \n" +
      "and  ((((match(c.concept_name) against(?1 in boolean mode) ) or\n" +
      "(match(cs.concept_synonym_name) against(?1 in boolean mode))) or (c.concept_id=?2 or c.concept_code=?2))\n" +
      "group by d.domain, d.domain_display, d.domain_desc, d.db_type, d.concept_id\n" +
      "order by d.domain")
  List<DomainInfo> findAllDomainSearchResults(String keyword, String query);
}
