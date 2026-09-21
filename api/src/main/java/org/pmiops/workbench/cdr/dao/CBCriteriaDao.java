package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * Some of our trees are polyhierarchical(SNOMED and DRUG). Since a node may exist multiple times in
 * this scenario with the same concept_id we only want to return it once from a user search. The
 * query to rank/min this to a single row was expensive. Instead, when building the cb_criteria
 * table(CDR indexing build) we add domain_rank1 to the full_text column for the first node that
 * matches in the tree for a specific concept_id. This allows us to use the full text index and
 * makes the query much faster.
 */
public interface CBCriteriaDao extends CrudRepository<DbCriteria, Long>, CustomCBCriteriaDao {
  @Query(value = "SELECT * FROM INFORMATION_SCHEMA.INNODB_FT_DEFAULT_STOPWORD", nativeQuery = true)
  List<String> findMySQLStopWords();
}
