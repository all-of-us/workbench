package org.pmiops.workbench.api;

import com.google.common.base.Strings;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.OptimisticLockException;
import org.pmiops.workbench.cohortreview.AnnotationQueryBuilder;
import org.pmiops.workbench.cohortreview.CohortAnnotationDefinitionService;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.CohortAnnotationDefinition;
import org.pmiops.workbench.model.CohortAnnotationDefinitionListResponse;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CohortAnnotationDefinitionController implements CohortAnnotationDefinitionApiDelegate {

  private CohortAnnotationDefinitionService cohortAnnotationDefinitionService;
  private CohortReviewService cohortReviewService;
  private WorkspaceService workspaceService;
  private static final Logger log =
      Logger.getLogger(CohortAnnotationDefinitionController.class.getName());

  @Autowired
  CohortAnnotationDefinitionController(
      CohortAnnotationDefinitionService cohortAnnotationDefinitionService,
      CohortReviewService cohortReviewService,
      WorkspaceService workspaceService) {
    this.cohortAnnotationDefinitionService = cohortAnnotationDefinitionService;
    this.cohortReviewService = cohortReviewService;
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<CohortAnnotationDefinition> createCohortAnnotationDefinition(
      String workspaceNamespace,
      String workspaceId,
      Long cohortId,
      CohortAnnotationDefinition cohortAnnotationDefinition) {
    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    validateColumnName(cohortAnnotationDefinition.getColumnName());
    validateCohortExist(cohortId);
    validateDefinitionExists(cohortId, cohortAnnotationDefinition.getColumnName());

    cohortAnnotationDefinition.setCohortId(cohortId);
    cohortAnnotationDefinition.etag(Etags.fromVersion(0));
    CohortAnnotationDefinition response = save(cohortAnnotationDefinition);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteCohortAnnotationDefinition(
      String workspaceNamespace, String workspaceId, Long cohortId, Long annotationDefinitionId) {
    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    validateCohortExist(cohortId);
    // Validate that CohortAnnotationDefinition exist
    findCohortAnnotationDefinition(cohortId, annotationDefinitionId);

    cohortAnnotationDefinitionService.delete(annotationDefinitionId);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<CohortAnnotationDefinition> getCohortAnnotationDefinition(
      String workspaceNamespace, String workspaceId, Long cohortId, Long annotationDefinitionId) {
    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    validateCohortExist(cohortId);

    return ResponseEntity.ok(findCohortAnnotationDefinition(cohortId, annotationDefinitionId));
  }

  @Override
  public ResponseEntity<CohortAnnotationDefinitionListResponse> getCohortAnnotationDefinitions(
      String workspaceNamespace, String workspaceId, Long cohortId) {
    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    validateCohortExist(cohortId);

    List<CohortAnnotationDefinition> defs =
        cohortAnnotationDefinitionService.findByCohortId(cohortId);

    return ResponseEntity.ok(new CohortAnnotationDefinitionListResponse().items(defs));
  }

  @Override
  public ResponseEntity<CohortAnnotationDefinition> updateCohortAnnotationDefinition(
      String workspaceNamespace,
      String workspaceId,
      Long cohortId,
      Long annotationDefinitionId,
      CohortAnnotationDefinition cohortAnnotationDefinitionRequest) {
    String columnName = cohortAnnotationDefinitionRequest.getColumnName();

    // This also enforces registered auth domain.
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    validateColumnName(columnName);
    validateCohortExist(cohortId);

    CohortAnnotationDefinition cohortAnnotationDefinition =
        findCohortAnnotationDefinition(cohortId, annotationDefinitionId).columnName(columnName);

    validateEtag(cohortAnnotationDefinitionRequest.getEtag(), cohortAnnotationDefinition);
    validateDefinitionExists(cohortId, columnName);

    return ResponseEntity.ok(save(cohortAnnotationDefinition));
  }

  private CohortAnnotationDefinition findCohortAnnotationDefinition(
      Long cohortId, Long annotationDefinitionId) {
    return cohortAnnotationDefinitionService.findByCohortIdAndCohortAnnotationDefinitionId(
        cohortId, annotationDefinitionId);
  }

  private void validateCohortExist(long cohortId) {
    DbCohort cohort = cohortReviewService.findCohort(cohortId);
    if (cohort == null) {
      throw new NotFoundException(
          String.format("Not Found: No Cohort exists for cohortId: %s", cohortId));
    }
  }

  private CohortAnnotationDefinition save(CohortAnnotationDefinition cohortAnnotationDefinition) {
    try {
      return cohortAnnotationDefinitionService.save(cohortAnnotationDefinition);
    } catch (OptimisticLockException e) {
      log.log(Level.WARNING, "version conflict for cohort annotation definition update", e);
      throw new ConflictException(
          "Failed due to concurrent cohort annotation definition modification");
    }
  }

  private void validateColumnName(String columnName) {
    if (AnnotationQueryBuilder.RESERVED_COLUMNS.contains(columnName)) {
      throw new BadRequestException("Annotations are not allowed to be named " + columnName);
    } else if (columnName.toUpperCase().contains(AnnotationQueryBuilder.DESCENDING_PREFIX)) {
      throw new BadRequestException(
          "Annotations are not allowed to contain " + AnnotationQueryBuilder.DESCENDING_PREFIX);
    }
  }

  private void validateEtag(String eTag, CohortAnnotationDefinition cohortAnnotationDefinition) {
    if (Strings.isNullOrEmpty(eTag)) {
      throw new BadRequestException("missing required update field 'etag'");
    }
    if (!cohortAnnotationDefinition.getEtag().equals(eTag)) {
      throw new ConflictException(
          "Attempted to modify outdated cohort annotation definition version");
    }
  }

  private void validateDefinitionExists(Long cohortId, String columnName) {
    if (cohortAnnotationDefinitionService.definitionExists(cohortId, columnName)) {
      throw new ConflictException(
          String.format("Conflict: Cohort Annotation Definition name exists for: %s", columnName));
    }
  }
}
