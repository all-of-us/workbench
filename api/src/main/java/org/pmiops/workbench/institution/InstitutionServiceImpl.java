package org.pmiops.workbench.institution;

import static org.pmiops.workbench.access.AccessUtils.getAccessTierByShortNameOrThrow;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
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
import org.pmiops.workbench.db.model.DbInstitutionUserInstructions;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.DuaType;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionTierRequirement;
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
  private final InstitutionEmailDomainMapper institutionEmailDomainMapper;
  private final InstitutionEmailAddressMapper institutionEmailAddressMapper;
  private final InstitutionUserInstructionsMapper institutionUserInstructionsMapper;
  private final InstitutionTierRequirementMapper institutionTierRequirementMapper;
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
      InstitutionEmailDomainMapper institutionEmailDomainMapper,
      InstitutionEmailAddressMapper institutionEmailAddressMapper,
      InstitutionUserInstructionsMapper institutionUserInstructionsMapper,
      InstitutionTierRequirementMapper institutionTierRequirementMapper,
      PublicInstitutionDetailsMapper publicInstitutionDetailsMapper) {
    this.accessTierDao = accessTierDao;
    this.institutionDao = institutionDao;
    this.institutionEmailDomainDao = institutionEmailDomainDao;
    this.institutionEmailAddressDao = institutionEmailAddressDao;
    this.institutionUserInstructionsDao = institutionUserInstructionsDao;
    this.institutionTierRequirementDao = institutionTierRequirementDao;
    this.verifiedInstitutionalAffiliationDao = verifiedInstitutionalAffiliationDao;
    this.institutionMapper = institutionMapper;
    this.institutionEmailDomainMapper = institutionEmailDomainMapper;
    this.institutionEmailAddressMapper = institutionEmailAddressMapper;
    this.institutionUserInstructionsMapper = institutionUserInstructionsMapper;
    this.institutionTierRequirementMapper = institutionTierRequirementMapper;
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
    return StreamSupport.stream(institutionDao.findAll().spliterator(), false)
        .map(publicInstitutionDetailsMapper::dbToModel)
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
          "DataIntegrityException: Please check that you are not creating an Institute which already exists");
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

    // If the Institution has DUA Agreement that is restricted just to few researchers
    // Confirm if the email address is in the allowed email list
    if (institution.getDuaTypeEnum() != null
        && institution.getDuaTypeEnum().equals(DuaType.RESTRICTED)) {
      final boolean validated = institution.getEmailAddresses().contains(contactEmail);
      log.info(
          String.format(
              "Contact email '%s' validated against RESTRICTED-DUA institution '%s': address %s",
              contactEmail, institution.getShortName(), validated ? "MATCHED" : "DID NOT MATCH"));
      return validated;
    }

    // If Agreement Type is NULL assume DUA Agreement is MASTER
    // If Institution agreement type is master confirm if the contact email has valid/allowed domain
    final String contactEmailDomain = contactEmail.substring(contactEmail.indexOf("@") + 1);
    final boolean validated = institution.getEmailDomains().contains(contactEmailDomain);
    log.info(
        String.format(
            "Contact email '%s' validated against MASTER-DUA institution '%s': domain %s %s",
            contactEmail,
            institution.getShortName(),
            contactEmailDomain,
            validated ? "MATCHED" : "DID NOT MATCH"));
    return validated;
  }

  @Override
  public List<String> getEmailDomains(String institutionShortName) {
    return new ArrayList<>(
        institutionEmailDomainMapper.dbDomainsToStrings(
            institutionEmailDomainDao.getByInstitution(
                getDbInstitutionOrThrow(institutionShortName))));
  }

  @Override
  public List<String> getEmailAddresses(String institutionShortName) {
    return new ArrayList<>(
        institutionEmailAddressMapper.dbAddressesToStrings(
            institutionEmailAddressDao.getByInstitution(
                getDbInstitutionOrThrow(institutionShortName))));
  }

  @Override
  public List<InstitutionTierRequirement> getTierRequirements(String institutionShortName) {
    return new ArrayList<>(
        institutionTierRequirementMapper.dbToModel(
            new ArrayList<>(
                institutionTierRequirementDao.getByInstitution(
                    getDbInstitutionOrThrow(institutionShortName)))));
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

  private Institution toModel(DbInstitution dbInstitution) {
    return institutionMapper.dbToModel(dbInstitution, this);
  }

  private void populateAuxTables(
      final Institution modelInstitution, final DbInstitution dbInstitution) {
    setInstitutionEmailDomains(modelInstitution, dbInstitution);
    setInstitutionEmailAddresses(modelInstitution, dbInstitution);
    setInstitutionTierRequirement(modelInstitution, dbInstitution);

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
      final Institution modelInstitution, final DbInstitution dbInstitution) {
    institutionEmailDomainDao.deleteByInstitution(dbInstitution);
    institutionEmailDomainMapper
        .modelToDb(modelInstitution, dbInstitution)
        .forEach(institutionEmailDomainDao::save);
  }

  // note that this replaces all email addresses for this institution with the passed-in addresses
  private void setInstitutionEmailAddresses(
      final Institution modelInstitution, final DbInstitution dbInstitution) {
    institutionEmailAddressDao.deleteByInstitution(dbInstitution);
    institutionEmailAddressMapper
        .modelToDb(modelInstitution, dbInstitution)
        .forEach(institutionEmailAddressDao::save);
  }

  // note that this replaces all requirements for this institution with the passed-in requirements
  private void setInstitutionTierRequirement(
      final Institution modelInstitution, final DbInstitution dbInstitution) {
    List<DbAccessTier> dbAccessTiers = accessTierDao.findAll();
    institutionTierRequirementDao.deleteByInstitution(dbInstitution);
    institutionTierRequirementMapper
        .modelToDb(modelInstitution, dbInstitution, dbAccessTiers)
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
    if (institutionRequest.getDuaTypeEnum() == null) {
      // For Existing Institutions
      institutionRequest.setDuaTypeEnum(DuaType.MASTER);
    }
    if (institutionRequest.getOrganizationTypeEnum() == null) {
      throw new BadRequestException("Organization type cannot be null");
    }
    if (institutionRequest.getOrganizationTypeEnum().equals(OrganizationType.OTHER)
        && Strings.isNullOrEmpty(institutionRequest.getOrganizationTypeOtherText())) {
      throw new BadRequestException("If organization type is OTHER, additional text is needed");
    }

    // If Agreement type is Restricted confirm Each Email address in list is valid
    if (institutionRequest.getDuaTypeEnum().equals(DuaType.RESTRICTED)
        && !institutionRequest.getEmailAddresses().isEmpty()) {
      institutionRequest
          .getEmailAddresses()
          .forEach(
              emailAddress -> {
                try {
                  new InternetAddress(emailAddress).validate();
                } catch (AddressException | NullPointerException ex) {
                  throw new BadRequestException("Email Address is not valid");
                }
              });
    }

    // All tier need to be present in API if tier requirement is present.
    if (institutionRequest.getTierRequirements() != null) {
      List<DbAccessTier> dbAccessTiers = accessTierDao.findAll();
      institutionRequest
          .getTierRequirements()
          .forEach(a -> getAccessTierByShortNameOrThrow(dbAccessTiers, a.getAccessTierShortName()));
    }
  }

  public Optional<Institution> getFirstMatchingInstitution(final String contactEmail) {
    return getInstitutions().stream()
        .filter(institution -> validateInstitutionalEmail(institution, contactEmail))
        .findFirst();
  }
}
