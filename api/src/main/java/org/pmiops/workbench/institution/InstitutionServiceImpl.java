package org.pmiops.workbench.institution;

import static org.pmiops.workbench.access.AccessTierService.REGISTERED_TIER_SHORT_NAME;
import static org.pmiops.workbench.access.AccessUtils.getAccessTierByShortNameOrThrow;
import static org.pmiops.workbench.institution.InstitutionUtils.getEmailAddressesByTierOrEmptySet;
import static org.pmiops.workbench.institution.InstitutionUtils.getEmailDomainsByTierOrEmptySet;
import static org.pmiops.workbench.institution.InstitutionUtils.getTierConfigByTier;

import com.google.common.base.Strings;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.jetbrains.annotations.Nullable;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.InstitutionEmailAddressDao;
import org.pmiops.workbench.db.dao.InstitutionEmailDomainDao;
import org.pmiops.workbench.db.dao.InstitutionTierRequirementDao;
import org.pmiops.workbench.db.dao.InstitutionUserInstructionsDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement.MembershipRequirement;
import org.pmiops.workbench.db.model.DbInstitutionUserInstructions;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionMembershipRequirement;
import org.pmiops.workbench.model.InstitutionTierConfig;
import org.pmiops.workbench.model.InstitutionUserInstructions;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.model.PublicInstitutionDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionServiceImpl implements InstitutionService {

  private static final Logger log = Logger.getLogger(InstitutionServiceImpl.class.getName());

  private static final String OPERATIONAL_USER_INSTITUTION_SHORT_NAME = "AouOps";

  private final AccessTierDao accessTierDao;

  private final InstitutionDao institutionDao;
  private final InstitutionEmailDomainDao institutionEmailDomainDao;
  private final InstitutionEmailAddressDao institutionEmailAddressDao;
  private final InstitutionUserInstructionsDao institutionUserInstructionsDao;
  private final InstitutionTierRequirementDao institutionTierRequirementDao;
  private final VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  private final InstitutionMapper institutionMapper;
  private final InstitutionUserInstructionsMapper institutionUserInstructionsMapper;
  private final InstitutionTierConfigMapper institutionTierConfigMapper;
  private final PublicInstitutionDetailsMapper publicInstitutionDetailsMapper;

  @Autowired
  InstitutionServiceImpl(
      AccessTierDao accessTierDao,
      InstitutionDao institutionDao,
      InstitutionEmailDomainDao institutionEmailDomainDao,
      InstitutionEmailAddressDao institutionEmailAddressDao,
      InstitutionUserInstructionsDao institutionUserInstructionsDao,
      InstitutionTierRequirementDao institutionTierRequirementDao,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao,
      InstitutionMapper institutionMapper,
      InstitutionUserInstructionsMapper institutionUserInstructionsMapper,
      InstitutionTierConfigMapper institutionTierConfigMapper,
      PublicInstitutionDetailsMapper publicInstitutionDetailsMapper) {
    this.accessTierDao = accessTierDao;
    this.institutionDao = institutionDao;
    this.institutionEmailDomainDao = institutionEmailDomainDao;
    this.institutionEmailAddressDao = institutionEmailAddressDao;
    this.institutionUserInstructionsDao = institutionUserInstructionsDao;
    this.institutionTierRequirementDao = institutionTierRequirementDao;
    this.verifiedInstitutionalAffiliationDao = verifiedInstitutionalAffiliationDao;
    this.institutionMapper = institutionMapper;
    this.institutionUserInstructionsMapper = institutionUserInstructionsMapper;
    this.institutionTierConfigMapper = institutionTierConfigMapper;
    this.publicInstitutionDetailsMapper = publicInstitutionDetailsMapper;
  }

  @Override
  public List<Institution> getInstitutions() {
    return StreamSupport.stream(institutionDao.findAll().spliterator(), false)
        .map(this::toModel)
        .sorted(Comparator.comparing(Institution::getDisplayName))
        .collect(Collectors.toList());
  }

  @Override
  public List<PublicInstitutionDetails> getPublicInstitutionDetails() {
    Map<Long, MembershipRequirement> registeredTierRequirementMap =
        StreamSupport.stream(institutionTierRequirementDao.findAll().spliterator(), false)
            .filter(t -> t.getAccessTier().getShortName().equals(REGISTERED_TIER_SHORT_NAME))
            .collect(
                Collectors.toMap(
                    t -> t.getInstitution().getInstitutionId(),
                    DbInstitutionTierRequirement::getMembershipRequirement));
    return StreamSupport.stream(institutionDao.findAll().spliterator(), false)
        .map(
            i ->
                publicInstitutionDetailsMapper.dbToModel(
                    i, registeredTierRequirementMap.get(i.getInstitutionId())))
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Institution> getInstitution(final String shortName) {
    return institutionDao.findOneByShortName(shortName).map(this::toModel);
  }

  @Override
  public DbInstitution getDbInstitutionOrThrow(final String shortName) {
    return institutionDao
        .findOneByShortName(shortName)
        .orElseThrow(
            () ->
                new NotFoundException(String.format("Could not find Institution '%s'", shortName)));
  }

  @Override
  public Institution createInstitution(final Institution institutionToCreate) {
    validateInstitution(institutionToCreate);
    try {
      final DbInstitution dbInstitution =
          institutionDao.save(institutionMapper.modelToDb(institutionToCreate));
      populateAuxTables(institutionToCreate, dbInstitution);
      return toModel(dbInstitution);
    } catch (DataIntegrityViolationException ex) {
      throw new ConflictException(
          "DataIntegrityException: Please check that you are not creating an Institute which already exists");
    }
  }

  @Override
  public void deleteInstitution(final String shortName) {
    final DbInstitution institution = getDbInstitutionOrThrow(shortName);
    if (verifiedInstitutionalAffiliationDao.findAllByInstitution(institution).isEmpty()) {
      // no verified user affiliations: safe to delete
      institutionDao.delete(institution);
    } else {
      throw new ConflictException(
          String.format(
              "Could not delete Institution '%s' because it has verified user affiliations",
              shortName));
    }
  }

  // The @Transactional annotation below is a hack to make the command line tool LoadInstitutions
  // work similarly to our API endpoints.  It's necessary because our command line tools don't set
  // up the proper transactional or session context.  See RW-4968
  @Transactional
  @Override
  public Optional<Institution> updateInstitution(
      final String shortName, final Institution institutionToUpdate) {
    validateInstitution(institutionToUpdate);
    try {
      return institutionDao
          .findOneByShortName(shortName)
          .map(DbInstitution::getInstitutionId)
          .map(
              dbId -> {
                // create new DB object, but mark it with the original's ID to indicate that this is
                // an update

                final DbInstitution dbObjectToUpdate =
                    institutionDao.save(
                        institutionMapper.modelToDb(institutionToUpdate).setInstitutionId(dbId));
                populateAuxTables(institutionToUpdate, dbObjectToUpdate);
                return toModel(dbObjectToUpdate);
              });
    } catch (DataIntegrityViolationException ex) {
      throw new ConflictException(
          "DataIntegrityException: Please check that you are not creating an Institute which already exists",
          ex);
    }
  }

  @Override
  public boolean validateAffiliation(
      @Nullable DbVerifiedInstitutionalAffiliation dbAffiliation, String contactEmail) {
    if (dbAffiliation == null) {
      return false;
    }
    return validateInstitutionalEmail(toModel(dbAffiliation.getInstitution()), contactEmail);
  }

  @Override
  public boolean validateInstitutionalEmail(Institution institution, String contactEmail) {
    try {
      // TODO RW-4489: UserService should handle initial email validation
      new InternetAddress(contactEmail).validate();
    } catch (AddressException e) {
      log.info(
          String.format(
              "Contact email '%s' validation threw an AddressException: %s",
              contactEmail, e.getMessage()));
      return false;
    } catch (NullPointerException e) {
      log.info(
          String.format(
              "Contact email '%s' validation threw a NullPointerException", contactEmail));
      return false;
    }

    // As of now, RT's short name is hard coded in AccessTierService. We may need a better way
    // to pull RT short name from config or database.
    final String accessTierShortName = REGISTERED_TIER_SHORT_NAME;
    final InstitutionTierConfig tierConfig =
        getTierConfigByTier(institution, accessTierShortName)
            .orElseThrow(
                () -> {
                  String errMsg =
                      String.format(
                          "Institutional tier config not found for institution '%s' and tier '%s'",
                          institution.getShortName(), accessTierShortName);
                  return new ServerErrorException(errMsg);
                });

    final boolean validated;
    final String logMsg;
    switch (tierConfig.getMembershipRequirement()) {
      case NO_ACCESS:
        validated = false;
        logMsg =
            String.format(
                "Cannot validate email because the membership requirement for institution '%s' and tier '%s' is NO_ACCESS",
                institution.getShortName(), accessTierShortName);
        break;
      case ADDRESSES:
        validated =
            getEmailAddressesByTierOrEmptySet(institution, accessTierShortName)
                .contains(contactEmail);
        logMsg =
            String.format(
                "Contact email '%s' validated against '%s' tier with ADDRESSES requirement: "
                    + "'%s': address %s",
                contactEmail,
                accessTierShortName,
                institution.getShortName(),
                validated ? "MATCHED" : "DID NOT MATCH");
        break;
      case DOMAINS:
        final String contactEmailDomain = contactEmail.substring(contactEmail.indexOf("@") + 1);
        validated =
            getEmailDomainsByTierOrEmptySet(institution, accessTierShortName)
                .contains(contactEmailDomain);
        logMsg =
            String.format(
                "Contact email '%s' validated against '%s' tier with DOMAINS requirement '%s': "
                    + "domain %s %s",
                contactEmail,
                accessTierShortName,
                institution.getShortName(),
                contactEmailDomain,
                validated ? "MATCHED" : "DID NOT MATCH");
        break;
      default:
        validated = false;
        logMsg =
            String.format(
                "Cannot validate email because institution '%s' does not have a membership requirement for tier '%s'",
                institution.getShortName(), accessTierShortName);
        break;
    }

    log.info(logMsg);
    return validated;
  }

  @Override
  public Optional<String> getInstitutionUserInstructions(final String shortName) {
    return institutionUserInstructionsDao
        .getByInstitution(getDbInstitutionOrThrow(shortName))
        .map(DbInstitutionUserInstructions::getUserInstructions);
  }

  @Override
  public void setInstitutionUserInstructions(final InstitutionUserInstructions userInstructions) {
    final DbInstitution dbInstitution =
        getDbInstitutionOrThrow(userInstructions.getInstitutionShortName());

    final DbInstitutionUserInstructions dbInstructions =
        institutionUserInstructionsMapper.modelToDb(userInstructions, dbInstitution);

    // if a DbInstitutionUserInstructions entry already exists for this Institution, retrieve its ID
    // so the call to save() replaces it

    institutionUserInstructionsDao
        .getByInstitution(dbInstructions.getInstitution())
        .ifPresent(
            existingDbEntry ->
                dbInstructions.setInstitutionUserInstructionsId(
                    existingDbEntry.getInstitutionUserInstructionsId()));

    institutionUserInstructionsDao.save(dbInstructions);
  }

  @Override
  @Transactional // TODO: understand why this is necessary
  public boolean deleteInstitutionUserInstructions(final String shortName) {
    final DbInstitution institution = getDbInstitutionOrThrow(shortName);
    return institutionUserInstructionsDao.deleteByInstitution(institution) > 0;
  }

  @Override
  public boolean validateOperationalUser(DbInstitution institution) {
    return institution != null
        && institution.getShortName().equals(OPERATIONAL_USER_INSTITUTION_SHORT_NAME);
  }

  @Override
  public Optional<Institution> getByUser(DbUser user) {
    return verifiedInstitutionalAffiliationDao
        .findFirstByUser(user)
        .map(DbVerifiedInstitutionalAffiliation::getInstitution)
        .map(dbi -> institutionMapper.dbToModel(dbi, this));
  }

  @Override
  public List<InstitutionTierConfig> getTierConfigs(String institutionShortName) {
    DbInstitution dbInstitution = getDbInstitutionOrThrow(institutionShortName);
    Map<String, Set<String>> emailDomainMapByTier =
        institutionEmailDomainDao.getByInstitution(dbInstitution).stream()
            .collect(
                Collectors.groupingBy(
                    d -> d.getAccessTier().getShortName(),
                    Collectors.mapping(
                        DbInstitutionEmailDomain::getEmailDomain,
                        Collectors.toCollection(TreeSet::new))));
    Map<String, Set<String>> emailAddressMapByTier =
        institutionEmailAddressDao.getByInstitution(dbInstitution).stream()
            .collect(
                Collectors.groupingBy(
                    d -> d.getAccessTier().getShortName(),
                    Collectors.mapping(
                        DbInstitutionEmailAddress::getEmailAddress,
                        Collectors.toCollection(TreeSet::new))));

    return institutionTierRequirementDao.getByInstitution(dbInstitution).stream()
        .map(
            t ->
                institutionTierConfigMapper.dbToTierConfigModel(
                    t,
                    emailAddressMapByTier.get(t.getAccessTier().getShortName()),
                    emailDomainMapByTier.get(t.getAccessTier().getShortName())))
        .collect(Collectors.toList());
  }

  private Institution toModel(DbInstitution dbInstitution) {
    return institutionMapper.dbToModel(dbInstitution, this);
  }

  private void populateAuxTables(
      final Institution modelInstitution, final DbInstitution dbInstitution) {
    List<DbAccessTier> dbAccessTiers = accessTierDao.findAll();
    setInstitutionEmailDomains(modelInstitution, dbInstitution, dbAccessTiers);
    setInstitutionEmailAddresses(modelInstitution, dbInstitution, dbAccessTiers);
    setInstitutionTierRequirement(modelInstitution, dbInstitution, dbAccessTiers);

    final String userInstructions = modelInstitution.getUserInstructions();
    if (!Strings.isNullOrEmpty(userInstructions)) {
      setInstitutionUserInstructions(
          new InstitutionUserInstructions()
              .institutionShortName(modelInstitution.getShortName())
              .instructions(userInstructions));
    } else {
      // Remove institution entry from institution_user_instructions table if user_instructions is
      // now empty or NULL
      institutionUserInstructionsDao
          .getByInstitution(dbInstitution)
          .ifPresent(
              userInstruction -> institutionUserInstructionsDao.deleteByInstitution(dbInstitution));
    }
  }

  // note that this replaces all email domains for this institution with the passed-in domains
  private void setInstitutionEmailDomains(
      final Institution modelInstitution,
      final DbInstitution dbInstitution,
      final List<DbAccessTier> dbAccessTiers) {
    institutionEmailDomainDao.deleteByInstitution(dbInstitution);
    for (DbAccessTier dbAccessTier : dbAccessTiers) {
      institutionTierConfigMapper
          .emailDomainsToDb(
              getEmailDomainsByTierOrEmptySet(modelInstitution, dbAccessTier.getShortName()),
              dbInstitution,
              dbAccessTier)
          .forEach(institutionEmailDomainDao::save);
    }
  }

  // note that this replaces all email addresses for this institution with the passed-in addresses
  private void setInstitutionEmailAddresses(
      final Institution modelInstitution,
      final DbInstitution dbInstitution,
      final List<DbAccessTier> dbAccessTiers) {
    institutionEmailAddressDao.deleteByInstitution(dbInstitution);
    for (DbAccessTier dbAccessTier : dbAccessTiers) {
      institutionTierConfigMapper
          .emailAddressesToDb(
              getEmailAddressesByTierOrEmptySet(modelInstitution, dbAccessTier.getShortName()),
              dbInstitution,
              dbAccessTier)
          .forEach(institutionEmailAddressDao::save);
    }
  }

  // note that this replaces all requirements for this institution with the passed-in requirements
  private void setInstitutionTierRequirement(
      final Institution modelInstitution,
      final DbInstitution dbInstitution,
      final List<DbAccessTier> dbAccessTiers) {
    institutionTierRequirementDao.deleteByInstitution(dbInstitution);
    // Make sure the delete succeeds. This is needed because delete record in db table seems not
    // cleanup the per_institution_per_tier constraint right away. Make one extra get call flushed
    // all pending writes including the constraint to be deleted.
    if (!institutionTierRequirementDao.getByInstitution(dbInstitution).isEmpty()) {
      throw new ServerErrorException(
          "Failed to cleanup existing tier requirements before replacing them");
    }
    institutionTierConfigMapper
        .tierConfigsToDbTierRequirements(
            modelInstitution.getTierConfigs(), dbInstitution, dbAccessTiers)
        .forEach(institutionTierRequirementDao::save);
  }

  // Take first 76 characters from display Name (with no spaces) and append 3 random number
  private String generateShortName(String displayName) {
    Random r = new Random();
    displayName = displayName.replaceAll("\\s", "");
    String shortName = displayName.length() > 76 ? displayName.substring(0, 76) : displayName;
    shortName = shortName + r.nextInt(500) + 1;
    return shortName;
  }

  private void validateInstitution(Institution institutionRequest) {
    // TODO(RW-7027): Figure out what validations we need for tier requirement change.
    if (Strings.isNullOrEmpty(institutionRequest.getDisplayName())) {
      throw new BadRequestException("Display Name cannot be empty");
    }
    if (Strings.isNullOrEmpty(institutionRequest.getShortName())) {
      institutionRequest.setShortName(generateShortName(institutionRequest.getDisplayName()));
    }
    if (institutionRequest.getOrganizationTypeEnum() == null) {
      throw new BadRequestException("Organization type cannot be null");
    }
    if (institutionRequest.getOrganizationTypeEnum().equals(OrganizationType.OTHER)
        && Strings.isNullOrEmpty(institutionRequest.getOrganizationTypeOtherText())) {
      throw new BadRequestException("If organization type is OTHER, additional text is needed");
    }

    // All tier need to be present in API if tier requirement is present.
    if (institutionRequest.getTierConfigs() != null) {
      List<DbAccessTier> dbAccessTiers = accessTierDao.findAll();
      for (InstitutionTierConfig institutionTierConfig : institutionRequest.getTierConfigs()) {
        // All tier need to be present in API if tier requirement is present.
        getAccessTierByShortNameOrThrow(
            dbAccessTiers, institutionTierConfig.getAccessTierShortName());
        // Each Email address in all tiers is valid.
        if (institutionTierConfig.getMembershipRequirement()
            == InstitutionMembershipRequirement.ADDRESSES) {
          validateEmailAddressOrThrow(
              getEmailAddressesByTierOrEmptySet(
                  institutionRequest, institutionTierConfig.getAccessTierShortName()));
        }
      }
    }
  }

  public Optional<Institution> getFirstMatchingInstitution(final String contactEmail) {
    return getInstitutions().stream()
        .filter(institution -> validateInstitutionalEmail(institution, contactEmail))
        .findFirst();
  }

  /** Validates list of email addresses, and throw {@link BadRequestException} if not valid. */
  private static void validateEmailAddressOrThrow(Set<String> emailAddresses) {
    emailAddresses.forEach(
        emailAddress -> {
          try {
            new InternetAddress(emailAddress).validate();
          } catch (AddressException | NullPointerException ex) {
            throw new BadRequestException("Email Address is not valid");
          }
        });
  }
}
