package org.pmiops.workbench.db.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.UnderservedPopulationEnum;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

/**
 * Static utility for converting between API enums and stored short values. All
 * stored enums should have an entry here, and the property on the @Entity model
 * should be Short or short (depending on nullability). A @Transient helper
 * method may also be added to the model class to handle conversion.
 *
 * Usage requirements:
 *
 * - Semantic mapping of enum values should never change without a migration
 *   process, as these short values correspond to values which may currently be
 *   stored in the database.
 * - Storage short values should never be reused (over time) within an enum.
 * - Before removing any enums values, there should be confirmation and possibly
 *   migration to ensure that value is not currently stored, else attempts to
 *   read this data may result in server errors.
 *
 * This utility is workaround to the default behavior of Spring Data JPA, which
 * allows you to auto-convert storage of either ordinals or string values of a
 * Java enum. Neither of these approaches is particularly robust as ordering
 * changes or enum value renames may result in data corruption.
 *
 * See RW-872 for more details.
 */
public final class StorageEnums {
  private static final BiMap<Authority, Short> CLIENT_TO_STORAGE_AUTHORITY =
      ImmutableBiMap.<Authority, Short>builder()
      .put(Authority.REVIEW_RESEARCH_PURPOSE, (short) 0)
      .put(Authority.DEVELOPER, (short) 1)
      .put(Authority.ACCESS_CONTROL_ADMIN, (short) 2)
      .build();

  public static Authority authorityFromStorage(Short authority) {
    return CLIENT_TO_STORAGE_AUTHORITY.inverse().get(authority);
  }

  public static Short authorityToStorage(Authority authority) {
    return CLIENT_TO_STORAGE_AUTHORITY.get(authority);
  }

  private static final BiMap<BillingProjectStatus, Short> CLIENT_TO_STORAGE_BILLING_PROJECT_STATUS =
      ImmutableBiMap.<BillingProjectStatus, Short>builder()
      .put(BillingProjectStatus.NONE, (short) 0)
      .put(BillingProjectStatus.PENDING, (short) 1)
      .put(BillingProjectStatus.READY, (short) 2)
      .put(BillingProjectStatus.ERROR, (short) 3)
      .build();

  public static BillingProjectStatus billingProjectStatusFromStorage(Short s) {
    return CLIENT_TO_STORAGE_BILLING_PROJECT_STATUS.inverse().get(s);
  }

  public static Short billingProjectStatusToStorage(BillingProjectStatus s) {
    return CLIENT_TO_STORAGE_BILLING_PROJECT_STATUS.get(s);
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

  private static final BiMap<UnderservedPopulationEnum, Short>
      CLIENT_TO_STORAGE_UNDERSERVED_POPULATION =
      ImmutableBiMap.<UnderservedPopulationEnum, Short>builder()
      .put(UnderservedPopulationEnum.RACE_AMERICAN_INDIAN_OR_ALASKA_NATIVE, (short) 0)
      .put(UnderservedPopulationEnum.RACE_ASIAN, (short) 1)
      .put(UnderservedPopulationEnum.RACE_BLACK_AFRICAN_OR_AFRICAN_AMERICAN, (short) 2)
      .put(UnderservedPopulationEnum.RACE_HISPANIC_OR_LATINO, (short) 3)
      .put(UnderservedPopulationEnum.RACE_MIDDLE_EASTERN_OR_NORTH_AFRICAN, (short) 4)
      .put(UnderservedPopulationEnum.RACE_NATIVE_HAWAIIAN_OR_PACIFIC_ISLANDER, (short) 5)
      .put(UnderservedPopulationEnum.RACE_MORE_THAN_ONE_RACE, (short) 6)
      .put(UnderservedPopulationEnum.AGE_CHILDREN, (short) 7)
      .put(UnderservedPopulationEnum.AGE_ADOLESCENTS, (short) 8)
      .put(UnderservedPopulationEnum.AGE_OLDER_ADULTS, (short) 9)
      .put(UnderservedPopulationEnum.AGE_ELDERLY, (short) 10)
      .put(UnderservedPopulationEnum.SEX_FEMALE, (short) 11)
      .put(UnderservedPopulationEnum.SEX_INTERSEX, (short) 12)
      .put(UnderservedPopulationEnum.SEXUAL_ORIENTATION_GAY, (short) 13)
      .put(UnderservedPopulationEnum.SEXUAL_ORIENTATION_LESBIAN, (short) 14)
      .put(UnderservedPopulationEnum.SEXUAL_ORIENTATION_BISEXUAL, (short) 15)
      .put(UnderservedPopulationEnum.SEXUAL_ORIENTATION_POLYSEXUAL_OMNISEXUAL_SAPIOSEXUAL_OR_PANSEXUAL, (short) 16)
      .put(UnderservedPopulationEnum.SEXUAL_ORIENTATION_ASEXUAL, (short) 17)
      .put(UnderservedPopulationEnum.SEXUAL_ORIENTATION_TWO_SPIRIT, (short) 18)
      .put(UnderservedPopulationEnum.SEXUAL_ORIENTATION_FIGURING_OUT_SEXUALITY, (short) 19)
      .put(UnderservedPopulationEnum.SEXUAL_ORIENTATION_MOSTLY_STRAIGHT, (short) 20)
      .put(UnderservedPopulationEnum.SEXUAL_ORIENTATION_DOES_NOT_THINK_OF_HAVING_SEXUALITY, (short) 21)
      .put(UnderservedPopulationEnum.SEXUAL_ORIENTATION_DOES_NOT_USE_LABELS, (short) 22)
      .put(UnderservedPopulationEnum.SEXUAL_ORIENTATION_DOES_NOT_KNOW_ANSWER, (short) 23)
      .put(UnderservedPopulationEnum.GENDER_IDENTITY_WOMAN, (short) 24)
      .put(UnderservedPopulationEnum.GENDER_IDENTITY_NON_BINARY, (short) 25)
      .put(UnderservedPopulationEnum.GENDER_IDENTITY_TRANSMAN, (short) 26)
      .put(UnderservedPopulationEnum.GENDER_IDENTITY_TRANSWOMAN, (short) 27)
      .put(UnderservedPopulationEnum.GENDER_IDENTITY_GENDERQUEER, (short) 28)
      .put(UnderservedPopulationEnum.GENDER_IDENTITY_GENDERFLUID, (short) 29)
      .put(UnderservedPopulationEnum.GENDER_IDENTITY_GENDER_VARIANT, (short) 30)
      .put(UnderservedPopulationEnum.GENDER_IDENTITY_QUESTIONING, (short) 31)
      .put(UnderservedPopulationEnum.GEOGRAPHY_URBAN_CLUSTERS, (short) 32)
      .put(UnderservedPopulationEnum.GEOGRAPHY_RURAL, (short) 33)
      .put(UnderservedPopulationEnum.DISABILITY_PHYSICAL, (short) 34)
      .put(UnderservedPopulationEnum.DISABILITY_MENTAL, (short) 35)
      .put(UnderservedPopulationEnum.ACCESS_TO_CARE_NOT_PAST_TWELVE_MONTHS, (short) 36)
      .put(UnderservedPopulationEnum.ACCESS_TO_CARE_CANNOT_OBTAIN_OR_PAY_FOR, (short) 37)
      .put(UnderservedPopulationEnum.EDUCATION_INCOME_LESS_THAN_HIGH_SCHOOL_GRADUATE, (short) 38)
      .put(UnderservedPopulationEnum.EDUCATION_INCOME_LESS_THAN_TWENTY_FIVE_THOUSAND_FOR_FOUR_PEOPLE, (short) 39)
      .build();

  public static UnderservedPopulationEnum underservedPopulationFromStorage(Short p) {
    return CLIENT_TO_STORAGE_UNDERSERVED_POPULATION.inverse().get(p);
  }

  public static Short underservedPopulationToStorage(UnderservedPopulationEnum p) {
    return CLIENT_TO_STORAGE_UNDERSERVED_POPULATION.get(p);
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

  /** Utility class. */
  private StorageEnums() {}
}
