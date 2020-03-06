package org.pmiops.workbench.db.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.ArchivalStatus;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.BillingAccountType;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.model.DisseminateResearchEnum;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.Education;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.Ethnicity;
import org.pmiops.workbench.model.GenderIdentity;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.NonAcademicAffiliation;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.model.Race;
import org.pmiops.workbench.model.ResearchOutcomeEnum;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SexAtBirth;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.Surveys;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.rdr.model.Gender;

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
  // AnnotationType
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

  // ArchivalStatus
  private static final BiMap<ArchivalStatus, Short> CLIENT_TO_STORAGE_ARCHIVAL_STATUS =
      ImmutableBiMap.<ArchivalStatus, Short>builder()
          .put(ArchivalStatus.LIVE, (short) 0)
          .put(ArchivalStatus.ARCHIVED, (short) 1)
          .build();

  public static ArchivalStatus archivalStatusFromStorage(Short s) {
    return CLIENT_TO_STORAGE_ARCHIVAL_STATUS.inverse().get(s);
  }

  public static Short archivalStatusToStorage(ArchivalStatus s) {
    return CLIENT_TO_STORAGE_ARCHIVAL_STATUS.get(s);
  }

  // Authority
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

  // BillingAccountType
  private static final BiMap<BillingAccountType, Short> CLIENT_TO_STORAGE_BILLING_ACCOUNT_TYPE =
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

  // BillingMigrationStatus
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

  // BillingStatus
  private static final BiMap<BillingStatus, Short> CLIENT_TO_STORAGE_BILLING_STATUS =
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

  // BufferEntryStatus
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

  // CohortStatus
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

  // DataAccessLevel
  private static final BiMap<DataAccessLevel, Short> CLIENT_TO_STORAGE_DATA_ACCESS_LEVEL =
      ImmutableBiMap.<DataAccessLevel, Short>builder()
          .put(DataAccessLevel.UNREGISTERED, (short) 0)
          .put(DataAccessLevel.REGISTERED, (short) 1)
          .put(DataAccessLevel.PROTECTED, (short) 2)
          .build();

  public static DataAccessLevel dataAccessLevelFromStorage(Short level) {
    return CLIENT_TO_STORAGE_DATA_ACCESS_LEVEL.inverse().get(level);
  }

  public static Short dataAccessLevelToStorage(DataAccessLevel level) {
    return CLIENT_TO_STORAGE_DATA_ACCESS_LEVEL.get(level);
  }

  // Degree
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

  // DisseminateResearch
  private static final BiMap<DisseminateResearchEnum, Short> CLIENT_TO_STORAGE_DISSEMINATE_RESEARCH =
      ImmutableBiMap.<DisseminateResearchEnum, Short>builder()
          .put(DisseminateResearchEnum.PUBLICATION_PEER_REVIEWED_JOURNALS, (short) 0)
          .put(DisseminateResearchEnum.PRESENATATION_SCIENTIFIC_CONFERENCES, (short) 1)
          .put(DisseminateResearchEnum.PRESS_RELEASE, (short) 2)
          .put(DisseminateResearchEnum.PUBLICATION_COMMUNITY_BASED_BLOG, (short) 3)
          .put(DisseminateResearchEnum.PUBLICATION_PERSONAL_BLOG, (short) 4)
          .put(DisseminateResearchEnum.SOCIAL_MEDIA, (short) 5)
          .put(DisseminateResearchEnum.PRESENTATION_ADVISORY_GROUPS, (short) 6)
          .put(DisseminateResearchEnum.OTHER, (short) 7)
          .build();

  public static DisseminateResearchEnum disseminateResearchEnumFromStorage(Short s) {
    return CLIENT_TO_STORAGE_DISSEMINATE_RESEARCH.inverse().get(s);
  }

  public static Short disseminateResearchToStorage(DisseminateResearchEnum s) {
    return CLIENT_TO_STORAGE_DISSEMINATE_RESEARCH.get(s);
  }

  // Domain
  private static final BiMap<Domain, Short> CLIENT_TO_STORAGE_DOMAIN =
      ImmutableBiMap.<Domain, Short>builder()
          .put(Domain.CONDITION, (short) 0)
          .put(Domain.DEATH, (short) 1)
          .put(Domain.DEVICE, (short) 2)
          .put(Domain.DRUG, (short) 3)
          .put(Domain.MEASUREMENT, (short) 4)
          .put(Domain.OBSERVATION, (short) 5)
          .put(Domain.PROCEDURE, (short) 6)
          .put(Domain.VISIT, (short) 7)
          .put(Domain.SURVEY, (short) 8)
          .put(Domain.PERSON, (short) 9)
          .put(Domain.PHYSICALMEASUREMENT, (short) 10)
          .build();

  // A mapping from our Domain enum to OMOP domain ID values.
  private static final BiMap<Domain, String> DOMAIN_ID_MAP =
      ImmutableBiMap.<Domain, String>builder()
          .put(Domain.CONDITION, "Condition")
          .put(Domain.DEATH, "Death")
          .put(Domain.DEVICE, "Device")
          .put(Domain.DRUG, "Drug")
          .put(Domain.MEASUREMENT, "Measurement")
          .put(Domain.OBSERVATION, "Observation")
          .put(Domain.PROCEDURE, "Procedure")
          .put(Domain.VISIT, "Visit")
          .put(Domain.SURVEY, "Survey")
          .put(Domain.PERSON, "Person")
          .put(Domain.PHYSICALMEASUREMENT, "Physical Measurement")
          .build();

  public static Domain domainFromStorage(Short domain) {
    return CLIENT_TO_STORAGE_DOMAIN.inverse().get(domain);
  }

  public static Short domainToStorage(Domain domain) {
    return CLIENT_TO_STORAGE_DOMAIN.get(domain);
  }

  public static String domainToDomainId(Domain domain) {
    return DOMAIN_ID_MAP.get(domain);
  }

  public static Domain domainIdToDomain(String domainId) {
    return DOMAIN_ID_MAP.inverse().get(domainId);
  }

  // Education
  private static final BiMap<Education, Short>
      CLIENT_TO_STORAGE_EDUCATION = ImmutableBiMap.<Education, Short>builder()
          .put(Education.NO_EDUCATION, (short) 0)
          .put(Education.GRADES_1_12, (short) 1)
          .put(Education.UNDERGRADUATE, (short) 2)
          .put(Education.COLLEGE_GRADUATE, (short) 3)
          .put(Education.MASTER, (short) 4)
          .put(Education.DOCTORATE, (short) 5)
          .build();

  public static Education educationFromStorage(Short education) {
    return CLIENT_TO_STORAGE_EDUCATION.inverse().get(education);
  }

  public static Short educationToStorage(Education education) {
    return CLIENT_TO_STORAGE_EDUCATION.get(education);
  }

  // EmailVerificationStatus
  private static final BiMap<EmailVerificationStatus, Short>
      CLIENT_TO_STORAGE_EMAIL_VERIFICATION_STATUS =
          ImmutableBiMap.<EmailVerificationStatus, Short>builder()
              .put(EmailVerificationStatus.UNVERIFIED, (short) 0)
              .put(EmailVerificationStatus.PENDING, (short) 1)
              .put(EmailVerificationStatus.SUBSCRIBED, (short) 2)
              .build();

  public static EmailVerificationStatus emailVerificationStatusFromStorage(Short emailVerificationStatus) {
    return CLIENT_TO_STORAGE_EMAIL_VERIFICATION_STATUS.inverse().get(emailVerificationStatus);
  }

  public static Short emailVerificationStatusToStorage(EmailVerificationStatus emailVerificationStatus) {
    return CLIENT_TO_STORAGE_EMAIL_VERIFICATION_STATUS.get(emailVerificationStatus);
  }

  // Ethnicity
  private static final BiMap<Ethnicity, Short> CLIENT_TO_STORAGE_ETHNICITY =
      ImmutableBiMap.<Ethnicity, Short>builder()
          .put(Ethnicity.HISPANIC, (short) 0)
          .put(Ethnicity.NOT_HISPANIC, (short) 1)
          .put(Ethnicity.PREFER_NO_ANSWER, (short) 2)
          .build();

  public static Ethnicity ethnicityFromStorage(Short ethnicity) {
    return CLIENT_TO_STORAGE_ETHNICITY.inverse().get(ethnicity);
  }

  public static Short ethnicityToStorage(Ethnicity ethnicity) {
    return CLIENT_TO_STORAGE_ETHNICITY.get(ethnicity);
  }

  // GenderIdentity
  private static final BiMap<GenderIdentity, Short> CLIENT_TO_STORAGE_GENDER_IDENTITY =
      ImmutableBiMap.<GenderIdentity, Short>builder()
          .put(GenderIdentity.MAN, (short) 0)
          .put(GenderIdentity.WOMAN, (short) 1)
          .put(GenderIdentity.NON_BINARY, (short) 2)
          .put(GenderIdentity.TRANSGENDER, (short) 3)
          .put(GenderIdentity.NONE_DESCRIBE_ME, (short) 4)
          .put(GenderIdentity.PREFER_NO_ANSWER, (short) 5)
          .build();

  public static GenderIdentity genderIdentityFromStorage(Short genderIdentity) {
    return CLIENT_TO_STORAGE_GENDER_IDENTITY.inverse().get(genderIdentity);
  }

  public static Short genderIdentityToStorage(GenderIdentity genderIdentity) {
    return CLIENT_TO_STORAGE_GENDER_IDENTITY.get(genderIdentity);
  }

  // InstitutionalRole
  private static final BiMap<InstitutionalRole, Short> CLIENT_TO_STORAGE_INSTITUTIONAL_ROLE =
      ImmutableBiMap.<InstitutionalRole, Short>builder()
          .put(InstitutionalRole.UNDERGRADUATE, (short) 0)
          .put(InstitutionalRole.TRAINEE, (short) 1)
          .put(InstitutionalRole.FELLOW, (short) 2)
          .put(InstitutionalRole.EARLY_CAREER, (short) 3)
          .put(InstitutionalRole.MID_CAREER, (short) 4)
          .put(InstitutionalRole.LATE_CAREER, (short) 5)
          .put(InstitutionalRole.PRE_DOCTORAL, (short) 6)
          .put(InstitutionalRole.POST_DOCTORAL, (short) 7)
          .put(InstitutionalRole.SENIOR_RESEARCHER, (short) 8)
          .put(InstitutionalRole.TEACHER, (short) 9)
          .put(InstitutionalRole.STUDENT, (short) 10)
          .put(InstitutionalRole.ADMIN, (short) 11)
          .put(InstitutionalRole.PROJECT_PERSONNEL, (short) 12)
          .put(InstitutionalRole.OTHER, (short) 13)
          .build();

  public static InstitutionalRole institutionalRoleFromStorage(Short institutionalRole) {
    return CLIENT_TO_STORAGE_INSTITUTIONAL_ROLE.inverse().get(institutionalRole);
  }

  public static Short institutionalRoleToStorage(InstitutionalRole institutionalRole) {
    return CLIENT_TO_STORAGE_INSTITUTIONAL_ROLE.get(institutionalRole);
  }

  // NonAcademicAffiliation
  private static final BiMap<NonAcademicAffiliation, Short> CLIENT_TO_STORAGE_NON_ACADEMIC_AFFILIATION =
      ImmutableBiMap.<NonAcademicAffiliation, Short>builder()
          .put(NonAcademicAffiliation.INDUSTRY, (short) 0)
          .put(NonAcademicAffiliation.EDUCATIONAL_INSTITUTION, (short) 1)
          .put(NonAcademicAffiliation.COMMUNITY_SCIENTIST, (short) 2)
          .put(NonAcademicAffiliation.FREE_TEXT, (short) 3)
          .build();

  public static NonAcademicAffiliation nonAcademicAffiliationFromStorage(Short nonAcademicAffiliation) {
    return CLIENT_TO_STORAGE_NON_ACADEMIC_AFFILIATION.inverse().get(nonAcademicAffiliation);
  }

  public static Short nonAcademicAffiliationToStorage(NonAcademicAffiliation nonAcademicAffiliation) {
    return CLIENT_TO_STORAGE_NON_ACADEMIC_AFFILIATION.get(nonAcademicAffiliation);
  }

  // OrganizationType
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

  // PrePackagedConceptSet
  private static final BiMap<PrePackagedConceptSetEnum, Short>
      CLIENT_TO_STORAGE_PRE_PACKAGED_CONCEPTSET =
      ImmutableBiMap.<PrePackagedConceptSetEnum, Short>builder()
          .put(PrePackagedConceptSetEnum.NONE, (short) 0)
          .put(PrePackagedConceptSetEnum.DEMOGRAPHICS, (short) 1)
          .put(PrePackagedConceptSetEnum.SURVEY, (short) 2)
          .put(PrePackagedConceptSetEnum.BOTH, (short) 3)
          .build();

  public static PrePackagedConceptSetEnum prePackagedConceptSetsFromStorage(Short conceptSet) {
    return CLIENT_TO_STORAGE_PRE_PACKAGED_CONCEPTSET.inverse().get(conceptSet);
  }

  public static Short prePackagedConceptSetsToStorage(PrePackagedConceptSetEnum conceptSet) {
    return CLIENT_TO_STORAGE_PRE_PACKAGED_CONCEPTSET.get(conceptSet);
  }

  // Race
  private static final BiMap<Race, Short> CLIENT_TO_STORAGE_RACE =
      ImmutableBiMap.<Race, Short>builder()
          .put(Race.AIAN, (short) 0)
          .put(Race.ASIAN, (short) 1)
          .put(Race.AA, (short) 2)
          .put(Race.NHOPI, (short) 3)
          .put(Race.WHITE, (short) 4)
          .put(Race.NONE, (short) 5)
          .put(Race.PREFER_NO_ANSWER, (short) 6)
          .build();
  
  public static final Race raceFromStorage(Short race) {
    return CLIENT_TO_STORAGE_RACE.inverse().get(race);
  }

  public static final Short raceToStorage(Race race) {
    return CLIENT_TO_STORAGE_RACE.get(race);
  }

  // ResearchOutcome
  private static final BiMap<ResearchOutcomeEnum, Short> CLIENT_TO_STORAGE_RESEARCH_OUTCOME =
      ImmutableBiMap.<ResearchOutcomeEnum, Short>builder()
          .put(ResearchOutcomeEnum.PROMOTE_HEALTHY_LIVING, (short) 0)
          .put(ResearchOutcomeEnum.IMPROVE_HEALTH_EQUALITY_UBR_POPULATIONS, (short) 1)
          .put(ResearchOutcomeEnum.IMPROVED_RISK_ASSESMENT, (short) 2)
          .put(ResearchOutcomeEnum.DECREASE_ILLNESS_BURDEN, (short) 3)
          .put(ResearchOutcomeEnum.PRECISION_INTERVENTION, (short) 4)
          .put(ResearchOutcomeEnum.NONE_APPLY, (short) 5)
          .build();

  public static ResearchOutcomeEnum researchOutcomeEnumFromStorage(Short s) {
    return CLIENT_TO_STORAGE_RESEARCH_OUTCOME.inverse().get(s);
  }

  public static Short researchOutcomeToStorage(ResearchOutcomeEnum s) {
    return CLIENT_TO_STORAGE_RESEARCH_OUTCOME.get(s);
  }

  // ReviewStatus
  private static final BiMap<ReviewStatus, Short> CLIENT_TO_STORAGE_REVIEW_STATUS =
      ImmutableBiMap.<ReviewStatus, Short>builder()
          .put(ReviewStatus.NONE, (short) 0)
          .put(ReviewStatus.CREATED, (short) 1)
          .build();

  public static ReviewStatus reviewStatusFromStorage(Short reviewStatus) {
    return CLIENT_TO_STORAGE_REVIEW_STATUS.inverse().get(reviewStatus);
  }

  public static Short reviewStatusToStorage(ReviewStatus reviewStatus) {
    return CLIENT_TO_STORAGE_REVIEW_STATUS.get(reviewStatus);
  }

  // SexAtBirth
  private static final BiMap<SexAtBirth, Short> CLIENT_TO_STORAGE_SEX_AT_BIRTH =
      ImmutableBiMap.<SexAtBirth, Short>builder()
          .put(SexAtBirth.MALE, (short) 0)
          .put(SexAtBirth.FEMALE, (short) 1)
          .put(SexAtBirth.INTERSEX, (short) 2)
          .put(SexAtBirth.NONE_OF_THESE_DESCRIBE_ME, (short) 3)
          .put(SexAtBirth.PREFER_NO_ANSWER, (short) 4)
          .build();

  public static final SexAtBirth sexAtBirthFromStorage(Short sexAtBirth) {
    return CLIENT_TO_STORAGE_SEX_AT_BIRTH.inverse().get(sexAtBirth);
  }

  public static final Short sexAtBirthToStorage(SexAtBirth sexAtBirth) {
    return CLIENT_TO_STORAGE_SEX_AT_BIRTH.get(sexAtBirth);
  }

  // SpecificPopulation
  private static final BiMap<SpecificPopulationEnum, Short> CLIENT_TO_STORAGE_SPECIFIC_POPULATION =
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
          .put(SpecificPopulationEnum.RACE_ASIAN, (short) 11)
          .put(SpecificPopulationEnum.RACE_AA, (short) 12)
          .put(SpecificPopulationEnum.RACE_HISPANIC, (short) 13)
          .put(SpecificPopulationEnum.RACE_AIAN, (short) 14)
          .put(SpecificPopulationEnum.RACE_MENA, (short) 15)
          .put(SpecificPopulationEnum.RACE_NHPI, (short) 16)
          .put(SpecificPopulationEnum.RACE_MORE_THAN_ONE, (short) 17)
          .put(SpecificPopulationEnum.AGE_CHILDREN, (short) 18)
          .put(SpecificPopulationEnum.AGE_ADOLESCENTS, (short) 19)
          .put(SpecificPopulationEnum.AGE_OLDER, (short) 20)
          .put(SpecificPopulationEnum.AGE_OLDER_MORE_THAN_75, (short) 21)
          .build();

  public static SpecificPopulationEnum specificPopulationFromStorage(Short s) {
    return CLIENT_TO_STORAGE_SPECIFIC_POPULATION.inverse().get(s);
  }

  public static Short specificPopulationToStorage(SpecificPopulationEnum s) {
    return CLIENT_TO_STORAGE_SPECIFIC_POPULATION.get(s);
  }

  // Surveys
  private static final BiMap<Surveys, Short> CLIENT_TO_STORAGE_SURVEY =
      ImmutableBiMap.<Surveys, Short>builder()
          .put(Surveys.THE_BASICS, (short) 0)
          .put(Surveys.LIFESTYLE, (short) 1)
          .put(Surveys.OVERALL_HEALTH, (short) 2)
          .build();

  public static Surveys surveysFromStorage(Short survey) {
    return CLIENT_TO_STORAGE_SURVEY.inverse().get(survey);
  }

  public static Short surveysToStorage(Surveys survey) {
    return CLIENT_TO_STORAGE_SURVEY.get(survey);
  }

  // WorkspaceAccessLevel
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

  // WorkspaceActiveStatus
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

  /** Utility class. */
  private DbStorageEnums() {}
}
