package org.pmiops.workbench.db.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.BillingAccountType;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;

/**
 * Static utility for converting between API enums and stored short values. All stored enums should
 * have an entry here, and the property on the @Entity model should be Short or short (depending on
 * nullability). A @Transient helper method may also be added to the model class to handle
 * conversion.
 *
 * <p>Usage requirements:
 *
 * <p>- Semantic mapping of enum values should never change without a migration process, as these
 * short values correspond to values which may currently be stored in the database. - Storage short
 * values should never be reused (over time) within an enum. - Before removing any enums values,
 * there should be confirmation and possibly migration to ensure that value is not currently stored,
 * else attempts to read this data may result in server errors.
 *
 * <p>This utility is workaround to the default behavior of Spring Data JPA, which allows you to
 * auto-convert storage of either ordinals or string values of a Java enum. Neither of these
 * approaches is particularly robust as ordering changes or enum value renames may result in data
 * corruption.
 *
 * <p>See RW-872 for more details.
 */
public final class DbStorageEnums {
  private static final BiMap<Authority, Short> CLIENT_TO_STORAGE_AUTHORITY =
      ImmutableBiMap.<Authority, Short>builder()
          .put(Authority.REVIEW_RESEARCH_PURPOSE, (short) 0)
          .put(Authority.DEVELOPER, (short) 1)
          .put(Authority.ACCESS_CONTROL_ADMIN, (short) 2)
          .put(Authority.FEATURED_WORKSPACE_ADMIN, (short) 3)
          .put(Authority.COMMUNICATIONS_ADMIN, (short) 4)
          .put(Authority.SECURITY_ADMIN, (short) 5)
          .put(Authority.INSTITUTION_ADMIN, (short) 6)
          .put(Authority.WORKSPACES_VIEW, (short) 7)
          .build();

  public static Authority authorityFromStorage(Short authority) {
    return CLIENT_TO_STORAGE_AUTHORITY.inverse().get(authority);
  }

  public static Short authorityToStorage(Authority authority) {
    return CLIENT_TO_STORAGE_AUTHORITY.get(authority);
  }

  private static final BiMap<BufferEntryStatus, Short>
      CLIENT_TO_STORAGE_BILLING_PROJECT_BUFFER_STATUS =
          ImmutableBiMap.<BufferEntryStatus, Short>builder()
              .put(BufferEntryStatus.CREATING, (short) 0)
              .put(BufferEntryStatus.ERROR, (short) 1)
              .put(BufferEntryStatus.AVAILABLE, (short) 2)
              .put(BufferEntryStatus.ASSIGNING, (short) 3)
              .put(BufferEntryStatus.ASSIGNED, (short) 4)
              .put(BufferEntryStatus.GARBAGE_COLLECTED, (short) 5)
              .build();

  public static BufferEntryStatus billingProjectBufferEntryStatusFromStorage(Short s) {
    return CLIENT_TO_STORAGE_BILLING_PROJECT_BUFFER_STATUS.inverse().get(s);
  }

  public static Short billingProjectBufferEntryStatusToStorage(BufferEntryStatus s) {
    return CLIENT_TO_STORAGE_BILLING_PROJECT_BUFFER_STATUS.get(s);
  }

  private static final BiMap<BillingMigrationStatus, Short>
      CLIENT_TO_STORAGE_BILLING_MIGRATION_STATUS =
          ImmutableBiMap.<BillingMigrationStatus, Short>builder()
              .put(BillingMigrationStatus.OLD, (short) 0)
              .put(BillingMigrationStatus.NEW, (short) 1)
              .put(BillingMigrationStatus.MIGRATED, (short) 2)
              .build();

  public static BillingMigrationStatus billingMigrationStatusFromStorage(Short s) {
    return CLIENT_TO_STORAGE_BILLING_MIGRATION_STATUS.inverse().get(s);
  }

  public static Short billingMigrationStatusToStorage(BillingMigrationStatus s) {
    return CLIENT_TO_STORAGE_BILLING_MIGRATION_STATUS.get(s);
  }

  private static final BiMap<EmailVerificationStatus, Short>
      CLIENT_TO_STORAGE_EMAIL_VERIFICATION_STATUS =
          ImmutableBiMap.<EmailVerificationStatus, Short>builder()
              .put(EmailVerificationStatus.UNVERIFIED, (short) 0)
              .put(EmailVerificationStatus.PENDING, (short) 1)
              .put(EmailVerificationStatus.SUBSCRIBED, (short) 2)
              .build();

  public static EmailVerificationStatus emailVerificationStatusFromStorage(Short s) {
    return CLIENT_TO_STORAGE_EMAIL_VERIFICATION_STATUS.inverse().get(s);
  }

  public static Short emailVerificationStatusToStorage(EmailVerificationStatus s) {
    return CLIENT_TO_STORAGE_EMAIL_VERIFICATION_STATUS.get(s);
  }

  private static final BiMap<WorkspaceAccessLevel, Short> CLIENT_TO_STORAGE_WORKSPACE_ACCESS =
      ImmutableBiMap.<WorkspaceAccessLevel, Short>builder()
          .put(WorkspaceAccessLevel.NO_ACCESS, (short) 0)
          .put(WorkspaceAccessLevel.READER, (short) 1)
          .put(WorkspaceAccessLevel.WRITER, (short) 2)
          .put(WorkspaceAccessLevel.OWNER, (short) 3)
          .build();

  public static WorkspaceAccessLevel workspaceAccessLevelFromStorage(Short level) {
    return CLIENT_TO_STORAGE_WORKSPACE_ACCESS.inverse().get(level);
  }

  public static Short workspaceAccessLevelToStorage(WorkspaceAccessLevel level) {
    return CLIENT_TO_STORAGE_WORKSPACE_ACCESS.get(level);
  }

  private static final BiMap<ReviewStatus, Short> CLIENT_TO_STORAGE_REVIEW_STATUS =
      ImmutableBiMap.<ReviewStatus, Short>builder()
          .put(ReviewStatus.NONE, (short) 0)
          .put(ReviewStatus.CREATED, (short) 1)
          .build();

  public static ReviewStatus reviewStatusFromStorage(Short s) {
    return CLIENT_TO_STORAGE_REVIEW_STATUS.inverse().get(s);
  }

  public static Short reviewStatusToStorage(ReviewStatus s) {
    return CLIENT_TO_STORAGE_REVIEW_STATUS.get(s);
  }

  private static final BiMap<CohortStatus, Short> CLIENT_TO_STORAGE_COHORT_STATUS =
      ImmutableBiMap.<CohortStatus, Short>builder()
          .put(CohortStatus.EXCLUDED, (short) 0)
          .put(CohortStatus.INCLUDED, (short) 1)
          .put(CohortStatus.NEEDS_FURTHER_REVIEW, (short) 2)
          .put(CohortStatus.NOT_REVIEWED, (short) 3)
          .build();

  public static CohortStatus cohortStatusFromStorage(Short s) {
    return CLIENT_TO_STORAGE_COHORT_STATUS.inverse().get(s);
  }

  public static Short cohortStatusToStorage(CohortStatus s) {
    return CLIENT_TO_STORAGE_COHORT_STATUS.get(s);
  }

  private static final BiMap<AnnotationType, Short> CLIENT_TO_STORAGE_ANNOTATION_TYPE =
      ImmutableBiMap.<AnnotationType, Short>builder()
          .put(AnnotationType.STRING, (short) 0)
          .put(AnnotationType.ENUM, (short) 1)
          .put(AnnotationType.DATE, (short) 2)
          .put(AnnotationType.BOOLEAN, (short) 3)
          .put(AnnotationType.INTEGER, (short) 4)
          .build();

  public static AnnotationType annotationTypeFromStorage(Short t) {
    return CLIENT_TO_STORAGE_ANNOTATION_TYPE.inverse().get(t);
  }

  public static Short annotationTypeToStorage(AnnotationType t) {
    return CLIENT_TO_STORAGE_ANNOTATION_TYPE.get(t);
  }

  private static final BiMap<WorkspaceActiveStatus, Short>
      CLIENT_TO_STORAGE_WORKSPACE_ACTIVE_STATUS =
          ImmutableBiMap.<WorkspaceActiveStatus, Short>builder()
              .put(WorkspaceActiveStatus.ACTIVE, (short) 0)
              .put(WorkspaceActiveStatus.DELETED, (short) 1)
              .put(WorkspaceActiveStatus.PENDING_DELETION_POST_1PPW_MIGRATION, (short) 2)
              .build();

  public static WorkspaceActiveStatus workspaceActiveStatusFromStorage(Short s) {
    return CLIENT_TO_STORAGE_WORKSPACE_ACTIVE_STATUS.inverse().get(s);
  }

  public static Short workspaceActiveStatusToStorage(WorkspaceActiveStatus s) {
    return CLIENT_TO_STORAGE_WORKSPACE_ACTIVE_STATUS.get(s);
  }

  public static final BiMap<SpecificPopulationEnum, Short> CLIENT_TO_STORAGE_SPECIFIC_POPULATION =
      ImmutableBiMap.<SpecificPopulationEnum, Short>builder()
          .put(SpecificPopulationEnum.RACE_ETHNICITY, (short) 0)
          .put(SpecificPopulationEnum.AGE_GROUPS, (short) 1)
          .put(SpecificPopulationEnum.SEX, (short) 2)
          .put(SpecificPopulationEnum.GENDER_IDENTITY, (short) 3)
          .put(SpecificPopulationEnum.SEXUAL_ORIENTATION, (short) 4)
          .put(SpecificPopulationEnum.GEOGRAPHY, (short) 5)
          .put(SpecificPopulationEnum.DISABILITY_STATUS, (short) 6)
          .put(SpecificPopulationEnum.ACCESS_TO_CARE, (short) 7)
          .put(SpecificPopulationEnum.EDUCATION_LEVEL, (short) 8)
          .put(SpecificPopulationEnum.INCOME_LEVEL, (short) 9)
          .put(SpecificPopulationEnum.OTHER, (short) 10)
          .build();

  public static SpecificPopulationEnum specificPopulationFromStorage(Short s) {
    return CLIENT_TO_STORAGE_SPECIFIC_POPULATION.inverse().get(s);
  }

  public static Short specificPopulationToStorage(SpecificPopulationEnum s) {
    return CLIENT_TO_STORAGE_SPECIFIC_POPULATION.get(s);
  }

  public static final BiMap<BillingStatus, Short> CLIENT_TO_STORAGE_BILLING_STATUS =
      ImmutableBiMap.<BillingStatus, Short>builder()
          .put(BillingStatus.ACTIVE, (short) 0)
          .put(BillingStatus.INACTIVE, (short) 1)
          .build();

  public static BillingStatus billingStatusFromStorage(Short s) {
    return CLIENT_TO_STORAGE_BILLING_STATUS.inverse().get(s);
  }

  public static Short billingStatusToStorage(BillingStatus s) {
    return CLIENT_TO_STORAGE_BILLING_STATUS.get(s);
  }

  public static final BiMap<BillingAccountType, Short> CLIENT_TO_STORAGE_BILLING_ACCOUNT_TYPE =
      ImmutableBiMap.<BillingAccountType, Short>builder()
          .put(BillingAccountType.FREE_TIER, (short) 0)
          .put(BillingAccountType.USER_PROVIDED, (short) 1)
          .build();

  public static BillingAccountType billingAccountTypeFromStorage(Short s) {
    return CLIENT_TO_STORAGE_BILLING_ACCOUNT_TYPE.inverse().get(s);
  }

  public static Short billingAccountTypeToStorage(BillingAccountType s) {
    return CLIENT_TO_STORAGE_BILLING_ACCOUNT_TYPE.get(s);
  }

  private static final BiMap<Degree, Short> CLIENT_TO_STORAGE_DEGREE =
      ImmutableBiMap.<Degree, Short>builder()
          .put(Degree.BA, (short) 0)
          .put(Degree.BS, (short) 1)
          .put(Degree.BSN, (short) 2)
          .put(Degree.EDD, (short) 3)
          .put(Degree.JD, (short) 4)
          .put(Degree.MA, (short) 5)
          .put(Degree.MBA, (short) 6)
          .put(Degree.MD, (short) 7)
          .put(Degree.ME, (short) 8)
          .put(Degree.MS, (short) 9)
          .put(Degree.MSN, (short) 10)
          .put(Degree.PHD, (short) 11)
          .put(Degree.NONE, (short) 12)
          .build();

  public static Degree degreeFromStorage(Short degree) {
    return CLIENT_TO_STORAGE_DEGREE.inverse().get(degree);
  }

  public static Short degreeToStorage(Degree degree) {
    return CLIENT_TO_STORAGE_DEGREE.get(degree);
  }

  private static final BiMap<OrganizationType, Short> CLIENT_TO_STORAGE_ORGANIZATION_TYPE =
      ImmutableBiMap.<OrganizationType, Short>builder()
          .put(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION, (short) 0)
          .put(OrganizationType.INDUSTRY, (short) 1)
          .put(OrganizationType.EDUCATIONAL_INSTITUTION, (short) 2)
          .put(OrganizationType.HEALTH_CENTER_NON_PROFIT, (short) 3)
          .put(OrganizationType.OTHER, (short) 4)
          .build();

  public static OrganizationType organizationTypeFromStorage(Short organizationType) {
    return CLIENT_TO_STORAGE_ORGANIZATION_TYPE.inverse().get(organizationType);
  }

  public static Short organizationTypeToStorage(OrganizationType organizationType) {
    return CLIENT_TO_STORAGE_ORGANIZATION_TYPE.get(organizationType);
  }

  /** Utility class. */
  private DbStorageEnums() {}
}
