package org.pmiops.workbench.cohortreview

import java.sql.Date
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.stream.Collectors
import org.apache.commons.lang3.StringUtils
import org.pmiops.workbench.cohortreview.util.PageRequest
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao
import org.pmiops.workbench.db.dao.CohortDao
import org.pmiops.workbench.db.dao.CohortReviewDao
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.CohortAnnotationDefinition
import org.pmiops.workbench.db.model.CohortAnnotationEnumValue
import org.pmiops.workbench.db.model.CohortReview
import org.pmiops.workbench.db.model.ParticipantCohortAnnotation
import org.pmiops.workbench.db.model.ParticipantCohortStatus
import org.pmiops.workbench.db.model.StorageEnums
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.model.AnnotationType
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.model.WorkspaceActiveStatus
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.String.Companion

@Service
class CohortReviewServiceImpl : CohortReviewService {

    private val cohortReviewDao: CohortReviewDao
    private val cohortDao: CohortDao
    private val participantCohortStatusDao: ParticipantCohortStatusDao
    private val participantCohortAnnotationDao: ParticipantCohortAnnotationDao
    private val cohortAnnotationDefinitionDao: CohortAnnotationDefinitionDao
    private val workspaceService: WorkspaceService

    @Autowired
    constructor(
            cohortReviewDao: CohortReviewDao,
            cohortDao: CohortDao,
            participantCohortStatusDao: ParticipantCohortStatusDao,
            participantCohortAnnotationDao: ParticipantCohortAnnotationDao,
            cohortAnnotationDefinitionDao: CohortAnnotationDefinitionDao,
            workspaceService: WorkspaceService) {
        this.cohortReviewDao = cohortReviewDao
        this.cohortDao = cohortDao
        this.participantCohortStatusDao = participantCohortStatusDao
        this.participantCohortAnnotationDao = participantCohortAnnotationDao
        this.cohortAnnotationDefinitionDao = cohortAnnotationDefinitionDao
        this.workspaceService = workspaceService
    }

    constructor() {}

    override fun findCohort(cohortId: Long): Cohort {
        return cohortDao.findOne(cohortId)
                ?: throw NotFoundException(
                        String.format("Not Found: No Cohort exists for cohortId: %s", cohortId))
    }

    override fun validateMatchingWorkspaceAndSetCdrVersion(
            workspaceNamespace: String,
            workspaceName: String,
            workspaceId: Long,
            accessRequired: WorkspaceAccessLevel): Workspace {
        val workspace = workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceName, accessRequired)
        if (workspace == null || workspace.workspaceId != workspaceId) {
            throw NotFoundException(
                    String.format(
                            "Not Found: No workspace matching workspaceNamespace: %s, workspaceId: %s",
                            workspaceNamespace, workspaceName))
        }
        return workspace
    }

    override fun enforceWorkspaceAccessLevel(
            workspaceNamespace: String, workspaceId: String, requiredAccess: WorkspaceAccessLevel): WorkspaceAccessLevel {
        return workspaceService.enforceWorkspaceAccessLevel(
                workspaceNamespace, workspaceId, requiredAccess)
    }

    override fun findCohortReview(cohortId: Long?, cdrVersionId: Long?): CohortReview {

        return cohortReviewDao.findCohortReviewByCohortIdAndCdrVersionId(cohortId!!, cdrVersionId!!)
                ?: throw NotFoundException(
                        String.format(
                                "Not Found: Cohort Review does not exist for cohortId: %s, cdrVersionId: %s",
                                cohortId, cdrVersionId))
    }

    override fun findCohortReview(cohortReviewId: Long?): CohortReview {

        return cohortReviewDao.findCohortReviewByCohortReviewId(cohortReviewId!!)
                ?: throw NotFoundException(
                        String.format(
                                "Not Found: Cohort Review does not exist for cohortReviewId: %s", cohortReviewId))
    }

    override fun findCohortReview(ns: String, firecloudName: String, cohortReviewId: Long?): CohortReview {

        return cohortReviewDao.findByNamespaceAndFirecloudNameAndCohortReviewId(
                ns, firecloudName, cohortReviewId!!)
                ?: throw NotFoundException(
                        String.format(
                                "Not Found: Cohort Review does not exist for namespace: %s, firecloudName: %s and cohortReviewId: %d",
                                ns, firecloudName, cohortReviewId))
    }

    override fun deleteCohortReview(cohortReview: CohortReview) {
        cohortReviewDao.delete(cohortReview)
    }

    override fun getRequiredWithCohortReviews(ns: String, firecloudName: String): List<CohortReview> {
        return cohortReviewDao.findByFirecloudNameAndActiveStatus(
                ns,
                firecloudName,
                StorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE)!!)
    }

    override fun saveCohortReview(cohortReview: CohortReview): CohortReview {
        return cohortReviewDao.save(cohortReview)
    }

    @Transactional
    override fun saveFullCohortReview(
            cohortReview: CohortReview, participantCohortStatuses: List<ParticipantCohortStatus>) {
        saveCohortReview(cohortReview)
        participantCohortStatusDao.saveParticipantCohortStatusesCustom(participantCohortStatuses)
    }

    override fun saveParticipantCohortStatus(
            participantCohortStatus: ParticipantCohortStatus): ParticipantCohortStatus {
        return participantCohortStatusDao.save(participantCohortStatus)
    }

    override fun findParticipantCohortStatus(
            cohortReviewId: Long?, participantId: Long?): ParticipantCohortStatus {
        return participantCohortStatusDao
                .findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                        cohortReviewId!!, participantId!!)
                ?: throw NotFoundException(
                        String.format(
                                "Not Found: Participant Cohort Status does not exist for cohortReviewId: %s, participantId: %s",
                                cohortReviewId, participantId))
    }

    override fun findAll(cohortReviewId: Long?, pageRequest: PageRequest): List<ParticipantCohortStatus> {
        return participantCohortStatusDao.findAll(cohortReviewId, pageRequest)
    }

    override fun findCount(cohortReviewId: Long?, pageRequest: PageRequest): Long? {
        return participantCohortStatusDao.findCount(cohortReviewId, pageRequest)
    }

    override fun saveParticipantCohortAnnotation(
            cohortReviewId: Long?, participantCohortAnnotation: ParticipantCohortAnnotation): ParticipantCohortAnnotation {
        val cohortAnnotationDefinition = findCohortAnnotationDefinition(
                participantCohortAnnotation.cohortAnnotationDefinitionId)

        validateParticipantCohortAnnotation(participantCohortAnnotation, cohortAnnotationDefinition)

        if (findParticipantCohortAnnotation(
                        cohortReviewId,
                        participantCohortAnnotation.cohortAnnotationDefinitionId,
                        participantCohortAnnotation.participantId) != null) {
            throw BadRequestException(
                    String.format(
                            "Bad Request: Cohort annotation definition exists for id: %s",
                            participantCohortAnnotation.cohortAnnotationDefinitionId))
        }
        return participantCohortAnnotationDao.save(participantCohortAnnotation)
    }

    override fun updateParticipantCohortAnnotation(
            annotationId: Long?,
            cohortReviewId: Long?,
            participantId: Long?,
            modifyRequest: ModifyParticipantCohortAnnotationRequest): ParticipantCohortAnnotation {
        val participantCohortAnnotation = participantCohortAnnotationDao.findByAnnotationIdAndCohortReviewIdAndParticipantId(
                annotationId!!, cohortReviewId!!, participantId!!)
                ?: throw NotFoundException(
                        String.format(
                                "Not Found: Participant Cohort Annotation does not exist for annotationId: %s, cohortReviewId: %s, participantId: %s",
                                annotationId, cohortReviewId, participantId))
        participantCohortAnnotation
                .annotationValueString(modifyRequest.getAnnotationValueString())
                .annotationValueEnum(modifyRequest.getAnnotationValueEnum())
                .annotationValueDateString(modifyRequest.getAnnotationValueDate())
                .annotationValueBoolean(modifyRequest.getAnnotationValueBoolean())
                .annotationValueInteger(modifyRequest.getAnnotationValueInteger())
        val cohortAnnotationDefinition = findCohortAnnotationDefinition(
                participantCohortAnnotation.cohortAnnotationDefinitionId)

        validateParticipantCohortAnnotation(participantCohortAnnotation, cohortAnnotationDefinition)

        return participantCohortAnnotationDao.save(participantCohortAnnotation)
    }

    override fun findCohortAnnotationDefinition(
            cohortAnnotationDefinitionId: Long?): CohortAnnotationDefinition {

        return cohortAnnotationDefinitionDao.findOne(cohortAnnotationDefinitionId)
                ?: throw NotFoundException(
                        String.format(
                                "Not Found: No cohort annotation definition found for id: %s",
                                cohortAnnotationDefinitionId))
    }

    override fun deleteParticipantCohortAnnotation(
            annotationId: Long?, cohortReviewId: Long?, participantId: Long?) {
        val participantCohortAnnotation = participantCohortAnnotationDao.findByAnnotationIdAndCohortReviewIdAndParticipantId(
                annotationId!!, cohortReviewId!!, participantId!!)
                ?: throw NotFoundException(
                        String.format(
                                "Not Found: No participant cohort annotation found for annotationId: %s," + " cohortReviewId: %s, participantId: %s",
                                annotationId, cohortReviewId, participantId))
        participantCohortAnnotationDao.delete(participantCohortAnnotation)
    }

    override fun findParticipantCohortAnnotation(
            cohortReviewId: Long?, cohortAnnotationDefinitionId: Long?, participantId: Long?): ParticipantCohortAnnotation? {
        return participantCohortAnnotationDao
                .findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId(
                        cohortReviewId!!, cohortAnnotationDefinitionId!!, participantId!!)
    }

    override fun findParticipantCohortAnnotations(
            cohortReviewId: Long?, participantId: Long?): List<ParticipantCohortAnnotation> {
        return participantCohortAnnotationDao.findByCohortReviewIdAndParticipantId(
                cohortReviewId!!, participantId!!)
    }

    /**
     * Helper method to validate that requested annotations are proper.
     *
     * @param participantCohortAnnotation
     */
    private fun validateParticipantCohortAnnotation(
            participantCohortAnnotation: ParticipantCohortAnnotation,
            cohortAnnotationDefinition: CohortAnnotationDefinition) {

        if (cohortAnnotationDefinition.annotationTypeEnum.equals(AnnotationType.BOOLEAN)) {
            if (participantCohortAnnotation.annotationValueBoolean == null) {
                throw createBadRequestException(
                        AnnotationType.BOOLEAN.name(),
                        participantCohortAnnotation.cohortAnnotationDefinitionId)
            }
        } else if (cohortAnnotationDefinition.annotationTypeEnum.equals(AnnotationType.STRING)) {
            if (StringUtils.isBlank(participantCohortAnnotation.annotationValueString)) {
                throw createBadRequestException(
                        AnnotationType.STRING.name(),
                        participantCohortAnnotation.cohortAnnotationDefinitionId)
            }
        } else if (cohortAnnotationDefinition.annotationTypeEnum.equals(AnnotationType.DATE)) {
            if (StringUtils.isBlank(participantCohortAnnotation.annotationValueDateString)) {
                throw createBadRequestException(
                        AnnotationType.DATE.name(),
                        participantCohortAnnotation.cohortAnnotationDefinitionId)
            }
            val sdf = SimpleDateFormat("yyyy-MM-dd")
            try {
                val date = Date(
                        sdf.parse(participantCohortAnnotation.annotationValueDateString).time)
                participantCohortAnnotation.annotationValueDate = date
            } catch (e: ParseException) {
                throw BadRequestException(
                        String.format(
                                "Bad Request: Please provide a valid %s value (%s) for annotation defintion id: %s",
                                AnnotationType.DATE.name(),
                                sdf.toPattern(),
                                participantCohortAnnotation.cohortAnnotationDefinitionId))
            }

        } else if (cohortAnnotationDefinition.annotationTypeEnum.equals(AnnotationType.INTEGER)) {
            if (participantCohortAnnotation.annotationValueInteger == null) {
                throw createBadRequestException(
                        AnnotationType.INTEGER.name(),
                        participantCohortAnnotation.cohortAnnotationDefinitionId)
            }
        } else if (cohortAnnotationDefinition.annotationTypeEnum.equals(AnnotationType.ENUM)) {
            if (StringUtils.isBlank(participantCohortAnnotation.annotationValueEnum)) {
                throw createBadRequestException(
                        AnnotationType.ENUM.name(),
                        participantCohortAnnotation.cohortAnnotationDefinitionId)
            }
            val enumValues = cohortAnnotationDefinition.enumValues.stream()
                    .filter { enumValue ->
                        participantCohortAnnotation
                                .annotationValueEnum == enumValue.name
                    }
                    .collect<List<CohortAnnotationEnumValue>, Any>(Collectors.toList())
            if (enumValues.isEmpty()) {
                throw createBadRequestException(
                        AnnotationType.ENUM.name(),
                        participantCohortAnnotation.cohortAnnotationDefinitionId)
            }
            participantCohortAnnotation.cohortAnnotationEnumValue = enumValues[0]
        }
    }

    /**
     * Helper method that creates a [BadRequestException] from the specified parameters.
     *
     * @param annotationType
     * @param cohortAnnotationDefinitionId
     * @return
     */
    private fun createBadRequestException(
            annotationType: String, cohortAnnotationDefinitionId: Long?): BadRequestException {
        return BadRequestException(
                String.format(
                        "Bad Request: Please provide a valid %s value for annotation defintion id: %s",
                        annotationType, cohortAnnotationDefinitionId))
    }
}
