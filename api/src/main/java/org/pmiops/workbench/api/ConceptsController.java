package org.pmiops.workbench.api;

import static org.pmiops.workbench.concept.ConceptService.STANDARD_CONCEPT_CODES;
import static org.pmiops.workbench.model.StandardConceptFilter.STANDARD_CONCEPTS;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbDomainInfo;
import org.pmiops.workbench.cdr.model.DbSurveyModule;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.DomainCount;
import org.pmiops.workbench.model.DomainCountsListResponse;
import org.pmiops.workbench.model.DomainCountsRequest;
import org.pmiops.workbench.model.DomainInfo;
import org.pmiops.workbench.model.DomainInfoResponse;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.model.SearchSurveysRequest;
import org.pmiops.workbench.model.SurveyModule;
import org.pmiops.workbench.model.SurveyQuestions;
import org.pmiops.workbench.model.SurveysResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConceptsController implements ConceptsApiDelegate {

  private static final Integer DEFAULT_MAX_RESULTS = 20;
  private static final int MAX_MAX_RESULTS = 1000;

  private final ConceptService conceptService;
  private final WorkspaceService workspaceService;

  @Autowired
  public ConceptsController(ConceptService conceptService, WorkspaceService workspaceService) {
    this.conceptService = conceptService;
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<DomainInfoResponse> getDomainInfo(
      String workspaceNamespace, String workspaceId) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(
        new DomainInfoResponse()
            .items(
                conceptService.getDomainInfo().stream()
                    .map(this::toClientDomainInfo)
                    .collect(Collectors.toList())));
  }

  @Override
  public ResponseEntity<List<SurveyQuestions>> searchSurveys(
      String workspaceNamespace, String workspaceId, SearchSurveysRequest request) {
    if (STANDARD_CONCEPTS.equals(request.getStandardConceptFilter())) {
      return ResponseEntity.ok(new ArrayList<>());
    }
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    Slice<DbCriteria> questionList =
        conceptService.searchSurveys(
            request.getQuery(),
            request.getSurveyName(),
            calculateResultLimit(request.getMaxResults()),
            Optional.ofNullable(request.getPageNumber()).orElse(0));
    return ResponseEntity.ok(
        questionList.getContent().stream()
            .map(this::toClientSurveyQuestions)
            .collect(Collectors.toList()));
  }

  @Override
  public ResponseEntity<SurveysResponse> getSurveyInfo(
      String workspaceNamespace, String workspaceId) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(
        new SurveysResponse()
            .items(
                conceptService.getSurveyInfo().stream()
                    .map(this::toClientSurveyModule)
                    .collect(Collectors.toList())));
  }

  @Override
  public ResponseEntity<DomainCountsListResponse> domainCounts(
      String workspaceNamespace, String workspaceId, DomainCountsRequest request) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    DomainCountsListResponse response = new DomainCountsListResponse();
    List<DomainCount> counts =
        conceptService.countDomains(
            request.getQuery(), request.getSurveyName(), request.getStandardConceptFilter());
    return ResponseEntity.ok(response.domainCounts(counts));
  }

  @Override
  public ResponseEntity<ConceptListResponse> searchConcepts(
      String workspaceNamespace, String workspaceId, SearchConceptsRequest request) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    Slice<DbConcept> concepts =
        conceptService.searchConcepts(
            request.getQuery(),
            request.getStandardConceptFilter(),
            request.getDomain(),
            calculateResultLimit(request.getMaxResults()),
            Optional.ofNullable(request.getPageNumber()).orElse(0));

    return ResponseEntity.ok(
        new ConceptListResponse()
            .items(
                concepts.getContent().stream()
                    .map(ConceptsController::toClientConcept)
                    .collect(Collectors.toList())));
  }

  private int calculateResultLimit(Integer limit) {
    int maxResults =
        Math.min(Optional.ofNullable(limit).orElse(DEFAULT_MAX_RESULTS), MAX_MAX_RESULTS);
    if (maxResults < 1) {
      throw new BadRequestException("Invalid value for maxResults: " + maxResults);
    }
    return maxResults;
  }

  public static Concept toClientConcept(DbConcept dbConcept) {
    return new Concept()
        .conceptClassId(dbConcept.getConceptClassId())
        .conceptCode(dbConcept.getConceptCode())
        .conceptName(dbConcept.getConceptName())
        .conceptId(dbConcept.getConceptId())
        .countValue(dbConcept.getCountValue())
        .domainId(dbConcept.getDomainId())
        .prevalence(dbConcept.getPrevalence())
        .standardConcept(STANDARD_CONCEPT_CODES.contains(dbConcept.getStandardConcept()))
        .vocabularyId(dbConcept.getVocabularyId())
        .conceptSynonyms(dbConcept.getSynonyms());
  }

  private SurveyQuestions toClientSurveyQuestions(DbCriteria dbCriteria) {
    return new SurveyQuestions()
        .conceptId(dbCriteria.getLongConceptId())
        .question(dbCriteria.getName());
  }

  private DomainInfo toClientDomainInfo(DbDomainInfo dbDomainInfo) {
    return new DomainInfo()
        .domain(dbDomainInfo.getDomainEnum())
        .name(dbDomainInfo.getName())
        .description(dbDomainInfo.getDescription())
        .allConceptCount(dbDomainInfo.getAllConceptCount())
        .standardConceptCount(dbDomainInfo.getStandardConceptCount())
        .participantCount(dbDomainInfo.getParticipantCount());
  }

  private SurveyModule toClientSurveyModule(DbSurveyModule dbSurveyModule) {
    return new SurveyModule()
        .conceptId(dbSurveyModule.getConceptId())
        .name(dbSurveyModule.getName())
        .description(dbSurveyModule.getDescription())
        .questionCount(dbSurveyModule.getQuestionCount())
        .participantCount(dbSurveyModule.getParticipantCount())
        .orderNumber(dbSurveyModule.getOrderNumber());
  }
}
