package org.pmiops.workbench.api;

import com.google.common.collect.ImmutableMultimap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.model.StandardConceptFilter;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConceptsController implements ConceptsApiDelegate {

  // TODO: consider putting this in CDM config, fetching it from there
  private static final ImmutableMultimap<Domain, String> DOMAIN_MAP =
      ImmutableMultimap.<Domain, String>builder()
          .put(Domain.CONDITION, "Condition")
          .put(Domain.CONDITION, "Condition/Meas")
          .put(Domain.CONDITION, "Condition/Device")
          .put(Domain.CONDITION, "Condition/Procedure")
          .put(Domain.DEVICE, "Device")
          .put(Domain.DEVICE, "Condition/Device")
          .put(Domain.DRUG, "Drug")
          .put(Domain.ETHNICITY, "Ethnicity")
          .put(Domain.GENDER, "Gender")
          .put(Domain.MEASUREMENT, "Measurement")
          .put(Domain.MEASUREMENT, "Meas/Procedure")
          .put(Domain.OBSERVATION, "Observation")
          .put(Domain.PROCEDURE, "Procedure")
          .put(Domain.PROCEDURE, "Meas/Procedure")
          .put(Domain.PROCEDURE, "Condition/Procedure")
          .put(Domain.RACE, "Race")
          .build();

  private static final Integer DEFAULT_MAX_RESULTS = 20;
  private static final int MAX_MAX_RESULTS = 1000;

  private final ConceptService conceptService;
  private final WorkspaceService workspaceService;

  private static final Function<org.pmiops.workbench.cdr.model.Concept, Concept> TO_CLIENT_CONCEPT =
      (concept) ->  new Concept()
            .conceptClassId(concept.getConceptClassId())
            .conceptCode(concept.getConceptCode())
            .conceptName(concept.getConceptName())
            .conceptId(concept.getConceptId())
            .countValue(concept.getCountValue())
            .domainId(concept.getDomainId())
            .prevalence(concept.getPrevalence())
            .standardConcept(ConceptService.STANDARD_CONCEPT_CODE.equals(
                concept.getStandardConcept()))
            .vocabularyId(concept.getVocabularyId());

  @Autowired
  public ConceptsController(ConceptService conceptService, WorkspaceService workspaceService) {
    this.conceptService = conceptService;
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<ConceptListResponse> searchConcepts(String workspaceNamespace,
      String workspaceId, SearchConceptsRequest request) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    Integer maxResults = request.getMaxResults();
    if (maxResults == null) {
      maxResults = DEFAULT_MAX_RESULTS;
    } else if (maxResults < 1) {
      throw new BadRequestException("Invalid value for maxResults: " + maxResults);
    } else if (maxResults > MAX_MAX_RESULTS) {
      maxResults = MAX_MAX_RESULTS;
    }
    StandardConceptFilter standardConceptFilter = request.getStandardConceptFilter();
    if (standardConceptFilter == null) {
      standardConceptFilter = StandardConceptFilter.ALL_CONCEPTS;
    }
    List<String> domainIds = null;
    if (request.getDomain() != null) {
      domainIds = DOMAIN_MAP.get(request.getDomain()).asList();
    }
    ConceptService.StandardConceptFilter convertedConceptFilter =
        ConceptService.StandardConceptFilter.valueOf(standardConceptFilter.name());
    if (request.getQuery().trim().isEmpty()) {
      throw new BadRequestException("Query must be non-whitespace");
    }

    // TODO: move Swagger codegen to common-api, pass request with modified values into service
    Slice<org.pmiops.workbench.cdr.model.Concept> concepts =
        conceptService.searchConcepts(request.getQuery(), convertedConceptFilter,
            request.getVocabularyIds(), domainIds, maxResults);
    ConceptListResponse response = new ConceptListResponse();
    response.setItems(concepts.getContent().stream().map(TO_CLIENT_CONCEPT)
        .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }
}
