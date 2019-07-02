package org.pmiops.workbench.db.dao;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.List;
import org.pmiops.workbench.db.model.ConceptSet;
import org.pmiops.workbench.model.Domain;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ConceptSetDao extends CrudRepository<ConceptSet, Long> {

  // TODO: consider putting this in CDM config, fetching it from there
  static final ImmutableMap<Domain, String> DOMAIN_TO_TABLE_NAME =
      ImmutableMap.<Domain, String>builder()
          .put(Domain.CONDITION, "condition_occurrence")
          .put(Domain.DEATH, "death")
          .put(Domain.DEVICE, "device_exposure")
          .put(Domain.DRUG, "drug_exposure")
          .put(Domain.MEASUREMENT, "measurement")
          .put(Domain.OBSERVATION, "observation")
          .put(Domain.PROCEDURE, "procedure_occurrence")
          .put(Domain.VISIT, "visit_occurrence")
          .build();

  List<ConceptSet> findByWorkspaceId(long workspaceId);

  List<ConceptSet> findByWorkspaceIdAndSurvey(long workspaceId, short surveyId);

  /** Returns the concept set in the workspace with the specified name, or null if there is none. */
  ConceptSet findConceptSetByNameAndWorkspaceId(String name, long workspaceId);

  @Modifying
  @Query(
      value =
          "INSERT INTO concept_set_concept_id(concept_set_id, concept_id) "
              + " SELECT (:toCsId), concept_id "
              + " FROM concept_set_concept_id "
              + " WHERE concept_set_id = (:fromCsId)",
      nativeQuery = true)
  void bulkCopyConceptIds(
      @Param("fromCsId") long fromConceptSetId, @Param("toCsId") long toConceptSetId);

  List<ConceptSet> findAllByConceptSetIdIn(Collection<Long> conceptSetIds);
}
