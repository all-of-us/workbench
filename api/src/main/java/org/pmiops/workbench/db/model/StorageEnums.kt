package org.pmiops.workbench.db.model

import com.google.common.collect.BiMap
import com.google.common.collect.ImmutableBiMap
import org.pmiops.workbench.db.model.BillingProjectBufferEntry.BillingProjectBufferStatus
import org.pmiops.workbench.db.model.Workspace.BillingMigrationStatus
import org.pmiops.workbench.model.AnnotationType
import org.pmiops.workbench.model.Authority
import org.pmiops.workbench.model.BillingAccountType
import org.pmiops.workbench.model.BillingStatus
import org.pmiops.workbench.model.CohortStatus
import org.pmiops.workbench.model.EmailVerificationStatus
import org.pmiops.workbench.model.ReviewStatus
import org.pmiops.workbench.model.SpecificPopulationEnum
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.model.WorkspaceActiveStatus

/**
 * Static utility for converting between API enums and stored short values. All stored enums should
 * have an entry here, and the property on the @Entity model should be Short or short (depending on
 * nullability). A @Transient helper method may also be added to the model class to handle
 * conversion.
 *
 *
 * Usage requirements:
 *
 *
 * - Semantic mapping of enum values should never change without a migration process, as these
 * short values correspond to values which may currently be stored in the database. - Storage short
 * values should never be reused (over time) within an enum. - Before removing any enums values,
 * there should be confirmation and possibly migration to ensure that value is not currently stored,
 * else attempts to read this data may result in server errors.
 *
 *
 * This utility is workaround to the default behavior of Spring Data JPA, which allows you to
 * auto-convert storage of either ordinals or string values of a Java enum. Neither of these
 * approaches is particularly robust as ordering changes or enum value renames may result in data
 * corruption.
 *
 *
 * See RW-872 for more details.
 */
object StorageEnums {
    private val CLIENT_TO_STORAGE_AUTHORITY = ImmutableBiMap.builder<Authority, Short>()
            .put(Authority.REVIEW_RESEARCH_PURPOSE, 0.toShort())
            .put(Authority.DEVELOPER, 1.toShort())
            .put(Authority.ACCESS_CONTROL_ADMIN, 2.toShort())
            .put(Authority.FEATURED_WORKSPACE_ADMIN, 3.toShort())
            .build()

    private val CLIENT_TO_STORAGE_BILLING_PROJECT_BUFFER_STATUS = ImmutableBiMap.builder<BillingProjectBufferStatus, Short>()
            .put(BillingProjectBufferStatus.CREATING, 0.toShort())
            .put(BillingProjectBufferStatus.ERROR, 1.toShort())
            .put(BillingProjectBufferStatus.AVAILABLE, 2.toShort())
            .put(BillingProjectBufferStatus.ASSIGNING, 3.toShort())
            .put(BillingProjectBufferStatus.ASSIGNED, 4.toShort())
            .put(BillingProjectBufferStatus.GARBAGE_COLLECTED, 5.toShort())
            .build()

    private val CLIENT_TO_STORAGE_BILLING_MIGRATION_STATUS = ImmutableBiMap.builder<BillingMigrationStatus, Short>()
            .put(BillingMigrationStatus.OLD, 0.toShort())
            .put(BillingMigrationStatus.NEW, 1.toShort())
            .put(BillingMigrationStatus.MIGRATED, 2.toShort())
            .build()

    private val CLIENT_TO_STORAGE_EMAIL_VERIFICATION_STATUS = ImmutableBiMap.builder<EmailVerificationStatus, Short>()
            .put(EmailVerificationStatus.UNVERIFIED, 0.toShort())
            .put(EmailVerificationStatus.PENDING, 1.toShort())
            .put(EmailVerificationStatus.SUBSCRIBED, 2.toShort())
            .build()

    private val CLIENT_TO_STORAGE_WORKSPACE_ACCESS = ImmutableBiMap.builder<WorkspaceAccessLevel, Short>()
            .put(WorkspaceAccessLevel.NO_ACCESS, 0.toShort())
            .put(WorkspaceAccessLevel.READER, 1.toShort())
            .put(WorkspaceAccessLevel.WRITER, 2.toShort())
            .put(WorkspaceAccessLevel.OWNER, 3.toShort())
            .build()

    private val CLIENT_TO_STORAGE_REVIEW_STATUS = ImmutableBiMap.builder<ReviewStatus, Short>()
            .put(ReviewStatus.NONE, 0.toShort())
            .put(ReviewStatus.CREATED, 1.toShort())
            .build()

    private val CLIENT_TO_STORAGE_COHORT_STATUS = ImmutableBiMap.builder<CohortStatus, Short>()
            .put(CohortStatus.EXCLUDED, 0.toShort())
            .put(CohortStatus.INCLUDED, 1.toShort())
            .put(CohortStatus.NEEDS_FURTHER_REVIEW, 2.toShort())
            .put(CohortStatus.NOT_REVIEWED, 3.toShort())
            .build()

    private val CLIENT_TO_STORAGE_ANNOTATION_TYPE = ImmutableBiMap.builder<AnnotationType, Short>()
            .put(AnnotationType.STRING, 0.toShort())
            .put(AnnotationType.ENUM, 1.toShort())
            .put(AnnotationType.DATE, 2.toShort())
            .put(AnnotationType.BOOLEAN, 3.toShort())
            .put(AnnotationType.INTEGER, 4.toShort())
            .build()

    private val CLIENT_TO_STORAGE_WORKSPACE_ACTIVE_STATUS = ImmutableBiMap.builder<WorkspaceActiveStatus, Short>()
            .put(WorkspaceActiveStatus.ACTIVE, 0.toShort())
            .put(WorkspaceActiveStatus.DELETED, 1.toShort())
            .put(WorkspaceActiveStatus.PENDING_DELETION_POST_1PPW_MIGRATION, 2.toShort())
            .build()

    val CLIENT_TO_STORAGE_SPECIFIC_POPULATION: BiMap<SpecificPopulationEnum, Short> = ImmutableBiMap.builder<SpecificPopulationEnum, Short>()
            .put(SpecificPopulationEnum.RACE_ETHNICITY, 0.toShort())
            .put(SpecificPopulationEnum.AGE_GROUPS, 1.toShort())
            .put(SpecificPopulationEnum.SEX, 2.toShort())
            .put(SpecificPopulationEnum.GENDER_IDENTITY, 3.toShort())
            .put(SpecificPopulationEnum.SEXUAL_ORIENTATION, 4.toShort())
            .put(SpecificPopulationEnum.GEOGRAPHY, 5.toShort())
            .put(SpecificPopulationEnum.DISABILITY_STATUS, 6.toShort())
            .put(SpecificPopulationEnum.ACCESS_TO_CARE, 7.toShort())
            .put(SpecificPopulationEnum.EDUCATION_LEVEL, 8.toShort())
            .put(SpecificPopulationEnum.INCOME_LEVEL, 9.toShort())
            .put(SpecificPopulationEnum.OTHER, 10.toShort())
            .build()

    val CLIENT_TO_STORAGE_BILLING_STATUS: BiMap<BillingStatus, Short> = ImmutableBiMap.builder<BillingStatus, Short>()
            .put(BillingStatus.ACTIVE, 0.toShort())
            .put(BillingStatus.INACTIVE, 1.toShort())
            .build()

    val CLIENT_TO_STORAGE_BILLING_ACCOUNT_TYPE: BiMap<BillingAccountType, Short> = ImmutableBiMap.builder<BillingAccountType, Short>()
            .put(BillingAccountType.FREE_TIER, 0.toShort())
            .put(BillingAccountType.USER_PROVIDED, 1.toShort())
            .build()

    fun authorityFromStorage(authority: Short?): Authority {
        return CLIENT_TO_STORAGE_AUTHORITY.inverse()[authority]
    }

    fun authorityToStorage(authority: Authority): Short? {
        return CLIENT_TO_STORAGE_AUTHORITY[authority]
    }

    fun billingProjectBufferStatusFromStorage(s: Short?): BillingProjectBufferStatus {
        return CLIENT_TO_STORAGE_BILLING_PROJECT_BUFFER_STATUS.inverse()[s]
    }

    fun billingProjectBufferStatusToStorage(s: BillingProjectBufferStatus): Short? {
        return CLIENT_TO_STORAGE_BILLING_PROJECT_BUFFER_STATUS[s]
    }

    fun billingMigrationStatusFromStorage(s: Short?): BillingMigrationStatus {
        return CLIENT_TO_STORAGE_BILLING_MIGRATION_STATUS.inverse()[s]
    }

    fun billingMigrationStatusToStorage(s: BillingMigrationStatus): Short? {
        return CLIENT_TO_STORAGE_BILLING_MIGRATION_STATUS[s]
    }

    fun emailVerificationStatusFromStorage(s: Short?): EmailVerificationStatus {
        return CLIENT_TO_STORAGE_EMAIL_VERIFICATION_STATUS.inverse()[s]
    }

    fun emailVerificationStatusToStorage(s: EmailVerificationStatus): Short? {
        return CLIENT_TO_STORAGE_EMAIL_VERIFICATION_STATUS[s]
    }

    fun workspaceAccessLevelFromStorage(level: Short?): WorkspaceAccessLevel {
        return CLIENT_TO_STORAGE_WORKSPACE_ACCESS.inverse()[level]
    }

    fun workspaceAccessLevelToStorage(level: WorkspaceAccessLevel): Short? {
        return CLIENT_TO_STORAGE_WORKSPACE_ACCESS[level]
    }

    fun reviewStatusFromStorage(s: Short?): ReviewStatus {
        return CLIENT_TO_STORAGE_REVIEW_STATUS.inverse()[s]
    }

    fun reviewStatusToStorage(s: ReviewStatus): Short? {
        return CLIENT_TO_STORAGE_REVIEW_STATUS[s]
    }

    fun cohortStatusFromStorage(s: Short?): CohortStatus {
        return CLIENT_TO_STORAGE_COHORT_STATUS.inverse()[s]
    }

    fun cohortStatusToStorage(s: CohortStatus): Short? {
        return CLIENT_TO_STORAGE_COHORT_STATUS[s]
    }

    fun annotationTypeFromStorage(t: Short?): AnnotationType {
        return CLIENT_TO_STORAGE_ANNOTATION_TYPE.inverse()[t]
    }

    fun annotationTypeToStorage(t: AnnotationType): Short? {
        return CLIENT_TO_STORAGE_ANNOTATION_TYPE[t]
    }

    fun workspaceActiveStatusFromStorage(s: Short?): WorkspaceActiveStatus {
        return CLIENT_TO_STORAGE_WORKSPACE_ACTIVE_STATUS.inverse()[s]
    }

    fun workspaceActiveStatusToStorage(s: WorkspaceActiveStatus): Short? {
        return CLIENT_TO_STORAGE_WORKSPACE_ACTIVE_STATUS[s]
    }

    fun specificPopulationFromStorage(s: Short?): SpecificPopulationEnum {
        return CLIENT_TO_STORAGE_SPECIFIC_POPULATION.inverse()[s]
    }

    fun specificPopulationToStorage(s: SpecificPopulationEnum): Short? {
        return CLIENT_TO_STORAGE_SPECIFIC_POPULATION[s]
    }

    fun billingStatusFromStorage(s: Short?): BillingStatus {
        return CLIENT_TO_STORAGE_BILLING_STATUS.inverse()[s]
    }

    fun billingStatusToStorage(s: BillingStatus): Short? {
        return CLIENT_TO_STORAGE_BILLING_STATUS[s]
    }

    fun billingAccountTypeFromStorage(s: Short?): BillingAccountType {
        return CLIENT_TO_STORAGE_BILLING_ACCOUNT_TYPE.inverse()[s]
    }

    fun billingAccountTypeToStorage(s: BillingAccountType): Short? {
        return CLIENT_TO_STORAGE_BILLING_ACCOUNT_TYPE[s]
    }
}
/** Utility class.  */
