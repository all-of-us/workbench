package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.DbDomain;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DbDomainDao extends CrudRepository<DbDomain, Long> {

    List<DbDomain> findByConceptIdNotNull();

    DbDomain findByConceptId(long conceptId);

    List<DbDomain> findByDbType(String db_type);

    List<DbDomain> findByDbTypeAndConceptIdNot(String db_type,Long concept_id);

        @Query(nativeQuery=true,value="select d.domain_id, d.domain_display, d.domain_desc,\n" +
                "d.db_type, d.domain_route,d.concept_id, count(distinct c.concept_id) as standard_concept_count\n" +
                "from db_domain d\n" +
                "join concept c on d.domain_id = c.domain_id\n" +
                "left join concept_synonym cs on c.concept_id=cs.concept_id \n" +
                "where d.db_type = 'domain_filter' and (c.count_value > 0 or c.source_count_value > 0) \n" +
                "and  ((((match(c.concept_name) against(?1 in boolean mode) ) or\n" +
                "(match(cs.concept_synonym_name) against(?1 in boolean mode))) and\n" +
                "c.standard_concept IN ('S', 'C') or (c.concept_id=?2 or c.concept_code=?2))\n" +
                "group by d.domain_id, d.domain_display, d.domain_desc, d.db_type, d.concept_id\n" +
                " \n" +
                "union\n" +
                " \n" +
                "select d.domain_id, d.domain_display, d.domain_desc, d.db_type, d.domain_route,\n" +
                "d.concept_id, count(distinct q.concept_id) as standard_concept_count from db_domain d\n" +
                "join achilles_results r on d.concept_id = r.stratum_1\n" +
                "join concept q on r.stratum_2 = q.concept_id\n" +
                "left join concept_synonym cs on q.concept_id=cs.concept_id \n" +
                "where d.db_type = 'survey' and r.analysis_id = 3110\n" +
                "and (((match(q.concept_name) against(?1 in boolean mode) ) or\n" +
                "(match(cs.concept_synonym_name) against(?1 in boolean mode) ) or\n" +
                "(q.concept_id=?2 or q.concept_code=?2)) or\n" +
                "(match(r.stratum_4) against(?1 in boolean mode)))\n" +
                "group by d.domain_id, d.domain_display, d.domain_desc, d.db_type, d.concept_id \n ")
    List<DbDomain> findDomainSearchResults(String keyword,String query);

    @Query(nativeQuery=true,value="select d.domain_id, d.domain_display, d.domain_desc, d.db_type,\n" +
            "d.domain_route, d.concept_id, count(DISTINCT c.concept_id) as standard_concept_count from db_domain d\n" +
            "join concept c on d.domain_id = c.domain_id\n" +
            "where d.db_type = 'domain_filter' and c.count_value > 0 and c.standard_concept IN ('S', 'C')\n" +
            "group by d.domain_id, d.domain_display, d.domain_desc, d.db_type, d.concept_id\n" +
            " \n" +
            "union\n" +
            " \n" +
            "select d.domain_id, d.domain_display, d.domain_desc, d.db_type, d.domain_route, d.concept_id,\n" +
            "count(q.concept_id) as standard_concept_count from db_domain d\n" +
            "join achilles_results r on d.concept_id = r.stratum_1\n" +
            "join concept q on r.stratum_2 = q.concept_id\n" +
            "where d.db_type = 'survey' and r.analysis_id = 3110\n" +
            "group by d.domain_id, d.domain_display, d.domain_desc, d.db_type, d.concept_id order by db_type ASC")
    List<DbDomain> findDomainTotals();

    @Query(nativeQuery=true, value="select d.domain_id, d.domain_display, d.domain_desc, d.db_type,\n" +
        "d.domain_route, d.concept_id, COUNT(DISTINCT c.concept_id) as all_concept_count,\n" +
        "SUM(c.standard_concept IN ('S', 'C')) as standard_concept_count,\n" +
        "dc.count_value participant_count from db_domain d\n" +
        "join concept dc on dc.concept_id = d.concept_id\n" +
        "left outer join concept c on d.domain_id = c.domain_id\n" +
        "where d.db_type = 'domain_filter'\n" +
        "group by d.domain_id, d.domain_display, d.domain_desc, d.db_type, d.concept_id\n" +
        "order by d.domain_id")
    List<DbDomain> findDomainFilterTotals();
}
