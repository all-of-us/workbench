package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.DbDomain;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DbDomainDao extends CrudRepository<DbDomain, Long> {
    List<DbDomain> findAll();
    
    DbDomain findByConceptId(long conceptId);

    List<DbDomain> findByDbType(String db_type);

    List<DbDomain> findByDbTypeAndAndConceptIdNotNull(String db_type);

    @Query(nativeQuery=true,value="select d.domain_id, d.domain_display, d.domain_desc, d.db_type, d.concept_id, sum(c.count_value) as count_value from db_domain d\n" +
            "join concept c on d.domain_id = c.domain_id\n" +
            "where d.db_type = 'domain_filter' and c.count_value > 0 and c.standard_concept = 'S'\n" +
            "and  (match(c.concept_name) against(?1 in boolean mode) )\n" +
            "group by d.domain_id, d.domain_display, d.domain_desc, d.db_type, d.concept_id\n" +
            " \n" +
            "union\n" +
            " \n" +
            "select d.domain_id, d.domain_display, d.domain_desc, d.db_type, d.concept_id, count(distinct q.concept_id) as count_value from db_domain d join achilles_results r on d.concept_id = r.stratum_1\n" +
            "join concept q on r.stratum_2 = q.concept_id\n" +
            "where d.db_type = 'survey' and r.analysis_id = 3110\n" +
            "and (match(q.concept_name) against(?1 in boolean mode) or\n" +
            " match(r.stratum_4) against(?1 in boolean mode) )\n" +
            "group by d.domain_id, d.domain_display, d.domain_desc, d.db_type, d.concept_id")
    List<DbDomain> findDomainSearchResults(String keyword);
}
