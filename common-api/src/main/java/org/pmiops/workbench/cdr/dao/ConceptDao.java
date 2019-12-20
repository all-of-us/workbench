package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ConceptDao extends CrudRepository<DbConcept, Long> {

  /**
   * Return the number of standard concepts in each vocabulary for the specified domain matching the
   * specified expression, matching concept name, synonym, ID, or code.
   *
   * @param matchExp SQL MATCH expression to match concept name or synonym
   * @param domainId domain ID to use when filtering concepts
   * @return per-vocabulary concept counts
   */
  @Query(
      value =
          "select c from DbConcept c\n"
              + "where (c.countValue > 0 or c.sourceCountValue > 0)\n"
              + "and matchConcept(c.conceptName, c.conceptCode, c.vocabularyId, c.synonymsStr, ?1) > 0\n"
              + "and c.standardConcept IN ('S', 'C') "
              + "and c.domainId = ?2")
  List<DbConcept> findStandardConcepts(String matchExp, String domainId, Pageable page);

  /**
   * Return the number of source concepts in each vocabulary for the specified domain matching the
   * specified expression, matching concept name, synonym, ID, or code.
   *
   * @param matchExp SQL MATCH expression to match concept name or synonym
   * @param domainId domain ID to use when filtering concepts
   * @return per-vocabulary concept counts
   */
  @Query(
      value =
          "select c from DbConcept c\n"
              + "where (c.countValue > 0 or c.sourceCountValue > 0)\n"
              + "and matchConcept(c.conceptName, c.conceptCode, c.vocabularyId, c.synonymsStr, ?1) > 0\n"
              + "and (c.standardConcept NOT IN ('S', 'C') or c.standardConcept is null) "
              + "and c.domainId = ?2")
  List<DbConcept> findSourceConcepts(String matchExp, String domainId, Pageable page);
}
