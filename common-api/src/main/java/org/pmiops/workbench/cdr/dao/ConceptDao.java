package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.VocabularyCount;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ConceptDao extends CrudRepository<Concept, Long> {

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
          "select c.vocabularyId as vocabularyId, count(distinct c.conceptId) as conceptCount from Concept c\n"
              + "where (c.countValue > 0 or c.sourceCountValue > 0) and\n"
              + "matchConcept(c.conceptName, c.conceptCode, c.vocabularyId, c.synonymsStr, ?1) > 0 and\n"
              + "c.standardConcept IN ('S', 'C') and\n"
              + "c.domainId = ?2\n"
              + "group by c.vocabularyId\n"
              + "order by c.vocabularyId\n")
  List<VocabularyCount> findConcepts(String matchExp, String domainId);
}
