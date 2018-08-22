package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.ConceptSet;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ConceptSetDao extends CrudRepository<ConceptSet, Long> {

  @Modifying
  @Query(
      value="INSERT INTO concept_set_concept_id(concept_set_id, concept_id) " +
          " SELECT (:toCsId), concept_id " +
          " FROM concept_set_concept_id " +
          " WHERE concept_set_id = (:fromCsId)",
      nativeQuery=true)
  void bulkCopyConceptIds(
      @Param("fromCsId") long fromConceptSetId, @Param("toCsId") long toConceptSetId);
}
