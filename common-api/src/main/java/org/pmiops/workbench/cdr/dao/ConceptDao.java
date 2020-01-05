package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ConceptDao extends CrudRepository<DbConcept, Long> {

  /**
   * Return standard or all concepts in each vocabulary for the specified domain matching the
   * specified expression, matching concept name, synonym, ID, or code.
   *
   * @param matchExp SQL MATCH expression to match concept name or synonym
   * @param conceptTypes can be 'S', 'C' or ''
   * @param domainIds domain IDs to use when filtering concepts
   * @return per-vocabulary concept counts
   */
  @Query(
      value =
          "select distinct c from DbConcept c "
              + "where (c.countValue > 0 or c.sourceCountValue > 0) "
              + "and matchConcept(c.conceptName, c.conceptCode, c.vocabularyId, c.synonymsStr, ?1) > 0 "
              + "and c.standardConcept IN (?2) "
              + "and c.domainId in (?3)")
  Page<DbConcept> findConcepts(
      String matchExp, List<String> conceptTypes, List<String> domainIds, Pageable page);

  /**
   * Return standard or all concepts in each vocabulary for the specified domain matching the
   * specified expression, matching concept name, synonym, ID, or code.
   *
   * @param conceptTypes can be 'S', 'C' or ''
   * @param domainIds domain IDs to use when filtering concepts
   * @return per-vocabulary concept counts
   */
  @Query(
      value =
          "select distinct c from DbConcept c "
              + "where (c.countValue > 0 or c.sourceCountValue > 0) "
              + "and c.standardConcept IN (?1) "
              + "and c.domainId in (?2)")
  Page<DbConcept> findConcepts(List<String> conceptTypes, List<String> domainIds, Pageable page);
}
