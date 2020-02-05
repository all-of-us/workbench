package org.pmiops.workbench.db.dao;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.List;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.model.Domain;
import org.springframework.data.repository.CrudRepository;

public interface ConceptSetDao extends CrudRepository<DbConceptSet, Long> {

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
          .put(Domain.PERSON, "person")
          .put(Domain.VISIT, "visit_occurrence")
          .put(Domain.SURVEY, "observation")
          .put(Domain.PHYSICALMEASUREMENT, "measurement")
          .build();

  List<DbConceptSet> findByWorkspaceId(long workspaceId);

  List<DbConceptSet> findByWorkspaceIdAndSurvey(long workspaceId, short surveyId);

  /** Returns the concept set in the workspace with the specified name, or null if there is none. */
  DbConceptSet findConceptSetByNameAndWorkspaceId(String name, long workspaceId);

  List<DbConceptSet> findAllByConceptSetIdIn(Collection<Long> conceptSetIds);

  int countByWorkspaceId(long workspaceId);
}
