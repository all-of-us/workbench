package org.pmiops.workbench.api

import com.google.common.base.Strings
import java.util.ArrayList
import java.util.function.Function
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.persistence.OptimisticLockException
import org.apache.commons.lang3.exception.ExceptionUtils
import org.pmiops.workbench.cohortreview.AnnotationQueryBuilder
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao
import org.pmiops.workbench.db.dao.CohortDao
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.CohortAnnotationEnumValue
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ConflictException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.model.CohortAnnotationDefinition
import org.pmiops.workbench.model.CohortAnnotationDefinitionListResponse
import org.pmiops.workbench.model.EmptyResponse
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import kotlin.String.Companion

@RestController
class CohortAnnotationDefinitionController @Autowired
internal constructor(
        private val cohortAnnotationDefinitionDao: CohortAnnotationDefinitionDao,
        private val cohortDao: CohortDao,
        private val workspaceService: WorkspaceService) : CohortAnnotationDefinitionApiDelegate {

    private fun validateColumnName(columnName: String?) {
        if (AnnotationQueryBuilder.RESERVED_COLUMNS.contains(columnName)) {
            throw BadRequestException("Annotations are not allowed to be named " + columnName!!)
        } else if (columnName!!.toUpperCase().contains(AnnotationQueryBuilder.DESCENDING_PREFIX)) {
            throw BadRequestException(
                    "Annotations are not allowed to contain " + AnnotationQueryBuilder.DESCENDING_PREFIX)
        }
    }

    fun createCohortAnnotationDefinition(
            workspaceNamespace: String,
            workspaceId: String,
            cohortId: Long?,
            request: CohortAnnotationDefinition): ResponseEntity<CohortAnnotationDefinition> {
        // This also enforces registered auth domain.
        workspaceService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)

        val cohort = findCohort(cohortId!!)
        // this validates that the user is in the proper workspace
        validateMatchingWorkspace(workspaceNamespace, workspaceId, cohort.workspaceId)
        request.setCohortId(cohortId)

        var cohortAnnotationDefinition: org.pmiops.workbench.db.model.CohortAnnotationDefinition = FROM_CLIENT_COHORT_ANNOTATION_DEFINITION.apply(request)
        validateColumnName(cohortAnnotationDefinition.columnName)

        val existingDefinition = cohortAnnotationDefinitionDao.findByCohortIdAndColumnName(
                cohortId, request.getColumnName())

        if (existingDefinition != null) {
            throw ConflictException(
                    String.format(
                            "Conflict: Cohort Annotation Definition name exists for: %s",
                            request.getColumnName()))
        }
        try {
            cohortAnnotationDefinition = cohortAnnotationDefinitionDao.save(cohortAnnotationDefinition)
        } catch (e: DataIntegrityViolationException) {
            throw BadRequestException("Bad Request: " + ExceptionUtils.getRootCause(e).message)
        }

        return ResponseEntity.ok<CohortAnnotationDefinition>(
                TO_CLIENT_COHORT_ANNOTATION_DEFINITION.apply(cohortAnnotationDefinition))
    }

    fun deleteCohortAnnotationDefinition(
            workspaceNamespace: String, workspaceId: String, cohortId: Long?, annotationDefinitionId: Long?): ResponseEntity<EmptyResponse> {
        // This also enforces registered auth domain.
        workspaceService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)

        val cohort = findCohort(cohortId!!)
        // this validates that the user is in the proper workspace
        validateMatchingWorkspace(workspaceNamespace, workspaceId, cohort.workspaceId)

        findCohortAnnotationDefinition(cohortId, annotationDefinitionId)

        cohortAnnotationDefinitionDao.delete(annotationDefinitionId)

        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    fun getCohortAnnotationDefinition(
            workspaceNamespace: String, workspaceId: String, cohortId: Long?, annotationDefinitionId: Long?): ResponseEntity<CohortAnnotationDefinition> {
        // This also enforces registered auth domain.
        workspaceService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)

        val cohort = findCohort(cohortId!!)
        // this validates that the user is in the proper workspace
        validateMatchingWorkspace(workspaceNamespace, workspaceId, cohort.workspaceId)

        val cohortAnnotationDefinition = findCohortAnnotationDefinition(cohortId, annotationDefinitionId)

        return ResponseEntity.ok<CohortAnnotationDefinition>(
                TO_CLIENT_COHORT_ANNOTATION_DEFINITION.apply(cohortAnnotationDefinition))
    }

    fun getCohortAnnotationDefinitions(
            workspaceNamespace: String, workspaceId: String, cohortId: Long?): ResponseEntity<CohortAnnotationDefinitionListResponse> {
        // This also enforces registered auth domain.
        workspaceService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)

        val cohort = findCohort(cohortId!!)
        // this validates that the user is in the proper workspace
        validateMatchingWorkspace(workspaceNamespace, workspaceId, cohort.workspaceId)

        val dbList = cohortAnnotationDefinitionDao.findByCohortId(cohortId)

        val responseList = CohortAnnotationDefinitionListResponse()
        responseList.setItems(
                dbList.stream().map(TO_CLIENT_COHORT_ANNOTATION_DEFINITION).collect(Collectors.toList<T>()))

        return ResponseEntity.ok<CohortAnnotationDefinitionListResponse>(responseList)
    }

    fun updateCohortAnnotationDefinition(
            workspaceNamespace: String,
            workspaceId: String,
            cohortId: Long?,
            annotationDefinitionId: Long?,
            cohortAnnotationDefinitionRequest: CohortAnnotationDefinition): ResponseEntity<CohortAnnotationDefinition> {
        // This also enforces registered auth domain.
        workspaceService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)

        val columnName = cohortAnnotationDefinitionRequest.getColumnName()
        validateColumnName(columnName)
        val cohort = findCohort(cohortId!!)
        // this validates that the user is in the proper workspace
        validateMatchingWorkspace(workspaceNamespace, workspaceId, cohort.workspaceId)

        var cohortAnnotationDefinition = findCohortAnnotationDefinition(cohortId, annotationDefinitionId)

        if (Strings.isNullOrEmpty(cohortAnnotationDefinitionRequest.getEtag())) {
            throw BadRequestException("missing required update field 'etag'")
        }
        val version = Etags.toVersion(cohortAnnotationDefinitionRequest.getEtag())
        if (cohortAnnotationDefinition.version != version) {
            throw ConflictException(
                    "Attempted to modify outdated cohort annotation definition version")
        }

        val existingDefinition = cohortAnnotationDefinitionDao.findByCohortIdAndColumnName(cohortId, columnName)

        if (existingDefinition != null) {
            throw ConflictException(
                    String.format("Conflict: Cohort Annotation Definition name exists for: %s", columnName))
        }

        cohortAnnotationDefinition.columnName(columnName)
        try {
            cohortAnnotationDefinition = cohortAnnotationDefinitionDao.save(cohortAnnotationDefinition)
        } catch (e: OptimisticLockException) {
            log.log(Level.WARNING, "version conflict for cohort annotation definition update", e)
            throw ConflictException("Failed due to concurrent cohort annotation modification")
        }

        return ResponseEntity.ok<CohortAnnotationDefinition>(
                TO_CLIENT_COHORT_ANNOTATION_DEFINITION.apply(cohortAnnotationDefinition))
    }

    private fun findCohortAnnotationDefinition(
            cohortId: Long?, annotationDefinitionId: Long?): org.pmiops.workbench.db.model.CohortAnnotationDefinition {

        return cohortAnnotationDefinitionDao.findByCohortIdAndCohortAnnotationDefinitionId(
                cohortId!!, annotationDefinitionId!!)
                ?: throw NotFoundException(
                        String.format(
                                "Not Found: No Cohort Annotation Definition exists for annotationDefinitionId: %s",
                                annotationDefinitionId))
    }

    private fun findCohort(cohortId: Long): Cohort {
        return cohortDao.findOne(cohortId)
                ?: throw NotFoundException(
                        String.format("Not Found: No Cohort exists for cohortId: %s", cohortId))
    }

    private fun validateMatchingWorkspace(
            workspaceNamespace: String, workspaceName: String, workspaceId: Long) {
        val workspace = workspaceService.getRequired(workspaceNamespace, workspaceName)
        if (workspace.workspaceId != workspaceId) {
            throw NotFoundException(
                    String.format(
                            "Not Found: No workspace matching workspaceNamespace: %s, workspaceId: %s",
                            workspaceNamespace, workspaceName))
        }
    }

    companion object {
        private val log = Logger.getLogger(CohortAnnotationDefinitionController::class.java.name)

        /**
         * Converter function from backend representation (used with Hibernate) to client representation
         * (generated by Swagger).
         */
        private val TO_CLIENT_COHORT_ANNOTATION_DEFINITION = { cohortAnnotationDefinition ->
            val enumValues = if (cohortAnnotationDefinition.enumValues == null)
                null
            else
                cohortAnnotationDefinition.enumValues.stream()
                        .map(Function<CohortAnnotationEnumValue, String> { it.name })
                        .collect(Collectors.toList<String>())
            CohortAnnotationDefinition()
                    .etag(Etags.fromVersion(cohortAnnotationDefinition.version))
                    .columnName(cohortAnnotationDefinition.columnName)
                    .cohortId(cohortAnnotationDefinition.cohortId)
                    .annotationType(cohortAnnotationDefinition.annotationTypeEnum)
                    .cohortAnnotationDefinitionId(
                            cohortAnnotationDefinition.cohortAnnotationDefinitionId)
                    .enumValues(enumValues)
        }

        private val FROM_CLIENT_COHORT_ANNOTATION_DEFINITION = { cohortAnnotationDefinition ->
            val dbCohortAnnotationDefinition = org.pmiops.workbench.db.model.CohortAnnotationDefinition()
                    .cohortId(cohortAnnotationDefinition.getCohortId())
                    .columnName(cohortAnnotationDefinition.getColumnName())
                    .annotationTypeEnum(cohortAnnotationDefinition.getAnnotationType())
            val enumValuesList = if (cohortAnnotationDefinition.getEnumValues() == null)
                ArrayList()
            else
                IntStream.range(0, cohortAnnotationDefinition.getEnumValues().size())
                        .mapToObj { i ->
                            CohortAnnotationEnumValue()
                                    .name(cohortAnnotationDefinition.getEnumValues().get(i))
                                    .order(i)
                                    .cohortAnnotationDefinition(dbCohortAnnotationDefinition)
                        }
                        .collect<List<CohortAnnotationEnumValue>, Any>(Collectors.toList())
            for (cohortAnnotationEnumValue in enumValuesList) {
                dbCohortAnnotationDefinition.enumValues.add(cohortAnnotationEnumValue)
            }
            dbCohortAnnotationDefinition
        }
    }
}
