package org.pmiops.workbench.institution;

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
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.InstitutionEmailAddressDao;
import org.pmiops.workbench.db.dao.InstitutionEmailDomainDao;
import org.pmiops.workbench.db.dao.InstitutionUserInstructionsDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionUserInstructions;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.DuaType;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionUserInstructions;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.model.PublicInstitutionDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InstitutionServiceImpl implements InstitutionService {

  private static final Logger log = Logger.getLogger(InstitutionServiceImpl.class.getName());

  private final InstitutionDao institutionDao;
  private final InstitutionUserInstructionsDao institutionUserInstructionsDao;
  private final VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  private final InstitutionEmailAddressDao institutionEmailAddressDao;
  private final InstitutionEmailDomainDao institutionEmailDomainDao;

  private final InstitutionMapper institutionMapper;
  private final InstitutionUserInstructionsMapper institutionUserInstructionsMapper;
  private final PublicInstitutionDetailsMapper publicInstitutionDetailsMapper;
  private final String OPERATIONAL_USER_INSTITUTION_SHORT_NAME = "AouOps";

  @Autowired
  InstitutionServiceImpl(
      InstitutionDao institutionDao,
      InstitutionUserInstructionsDao institutionUserInstructionsDao,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao,
      InstitutionEmailAddressDao institutionEmailAddressDao,
      InstitutionEmailDomainDao institutionEmailDomainDao,
      InstitutionMapper institutionMapper,
      InstitutionUserInstructionsMapper institutionUserInstructionsMapper,
      PublicInstitutionDetailsMapper publicInstitutionDetailsMapper) {
    this.institutionDao = institutionDao;
    this.institutionUserInstructionsDao = institutionUserInstructionsDao;
    this.verifiedInstitutionalAffiliationDao = verifiedInstitutionalAffiliationDao;
    this.institutionEmailAddressDao = institutionEmailAddressDao;
    this.institutionEmailDomainDao = institutionEmailDomainDao;
    this.institutionMapper = institutionMapper;
    this.institutionUserInstructionsMapper = institutionUserInstructionsMapper;
    this.publicInstitutionDetailsMapper = publicInstitutionDetailsMapper;
  }

  @Override
  public List<Institution> getInstitutions() {
    return StreamSupport.stream(institutionDao.findAll().spliterator(), false)
        .map(institution -> institutionMapper.dbToModel(institution))
        .sorted(Comparator.comparing(Institution::getDisplayName, String.CASE_INSENSITIVE_ORDER))
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
    return getDbInstitution(shortName).map(institution -> institutionMapper.dbToModel(institution));
  }

  @Override
  public DbInstitution getDbInstitutionOrThrow(final String shortName) {
    return getDbInstitution(shortName)
        .orElseThrow(
            () ->
                new NotFoundException(String.format("Could not find Institution '%s'", shortName)));
  }

  @Override
  public Optional<DbInstitutionUserInstructions> getDbInstitutionUserInstructions(
      String shortName) {
    return institutionUserInstructionsDao.getByInstitution_ShortName(shortName);
  }

  private Optional<DbInstitution> getDbInstitution(final String shortName) {
    return institutionDao.findOneByShortName(shortName);
  }

  @Override
  public Institution createInstitution(final Institution institutionToCreate) {
    validateInstitutionRequest(institutionToCreate);
    institutionToCreate.setShortName(createShortName(institutionToCreate.getDisplayName()));
    try {
      return institutionMapper.dbToModel(institutionDao.save(modelToDb(institutionToCreate)));
    } catch (DataIntegrityViolationException ex) {
      throw new ConflictException("Institution with the same name already exists");
    }
  }

  // Creates shortName by taking the first 3 characters of display Name and append 3 digit random
  // numbers
  // In case the display Name length < 3 append 3 digit random numbers to display Name
  private String createShortName(String displayName) {
    Random r = new Random();
    String shortName =
        (displayName.length() > 3 ? displayName.substring(0, 3) : displayName) + r.nextInt(500) + 1;
    return shortName;
  }

  // Validates that the create/update request does not have empty display Name/Organization Type
  private void validateInstitutionRequest(Institution institutionRequest) {
    if (StringUtils.isEmpty(institutionRequest.getDisplayName())) {
      throw new BadRequestException("Display Name cannot be empty");
    }
    if (institutionRequest.getDuaTypeEnum() == null) {
      // For Existing Institutions
      institutionRequest.setDuaTypeEnum(DuaType.MASTER);
    }
    if (institutionRequest.getOrganizationTypeEnum() == null) {
      throw new BadRequestException("Organization type cannot by null");
    }
    if (institutionRequest.getOrganizationTypeEnum().equals(OrganizationType.OTHER)
        && StringUtils.isEmpty(institutionRequest.getOrganizationTypeOtherText())) {
      throw new BadRequestException("If organization type is OTHER other text cannot be empty");
    }

    // If Agreement type is Individual confirm Each Email address in list is valid
    if (institutionRequest.getDuaTypeEnum().equals(DuaType.RESTRICTED)
        && !institutionRequest.getEmailAddresses().isEmpty()) {
      institutionRequest.getEmailAddresses().stream()
          .map(
              emailAddress -> {
                try {
                  new InternetAddress(emailAddress).validate();
                  return emailAddress;
                } catch (AddressException e) {
                  throw new BadRequestException("Email Address is not valid");
                }
              });
    }
  }

  private DbInstitution modelToDb(Institution institutionToCreate) {
    DbInstitution dbInstitutionToCreate = institutionMapper.modelToDb(institutionToCreate, this);
    if (!StringUtils.isEmpty(institutionToCreate.getUserInstructions())) {
      dbInstitutionToCreate.getUserInstructions().setInstitution(dbInstitutionToCreate);
    }
    return dbInstitutionToCreate;
  }

  private String generateShortName(String institutionDisplayName) {
    return "";
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
    validateInstitutionRequest(institutionToUpdate);
    return getDbInstitution(shortName)
        .map(this::deleteExistingEmailEntries)
        .map(DbInstitution::getInstitutionId)
        .map(dbId -> updateInstitution(institutionToUpdate, dbId))
        .map(institutionMapper::dbToModel);
  }

  /**
   * DbInstitute save was not updating the emailDomain or EmailAddress, it was just adding new
   * entries. Removing them first before adding them helped
   *
   * @param institutionToUpdate
   * @return
   */
  DbInstitution deleteExistingEmailEntries(DbInstitution institutionToUpdate) {
    institutionEmailDomainDao.deleteDbInstitutionEmailDomainByInstitution_ShortName(
        institutionToUpdate.getShortName());
    institutionEmailAddressDao.deleteDbInstitutionEmailAddressesByInstitution_ShortName(
        institutionToUpdate.getShortName());
    return institutionToUpdate;
  }

  /**
   * Update institution Id to the database institute and userInstruction object and save it
   *
   * @param institutionToUpdate Researcher updated Instiution Modal
   * @param dbId: Database Id for existing dbInstitution object
   * @return
   */
  DbInstitution updateInstitution(Institution institutionToUpdate, long dbId) {
    final DbInstitution dbObjectToUpdate = institutionMapper.modelToDb(institutionToUpdate, this);
    dbObjectToUpdate.setInstitutionId(dbId);
    dbObjectToUpdate.getUserInstructions().setInstitution(dbObjectToUpdate);
    DbInstitution dbInstitution = institutionDao.save(dbObjectToUpdate);

    // Modify and insert were fine but the transaction was not deleting entry from user institution
    // it was inserting null/blank values instead, hence explicitly deleting the entry from
    // DbUserInstructions
    if (StringUtils.isEmpty(institutionToUpdate.getUserInstructions())) {
      institutionUserInstructionsDao.deleteDbInstitutionUserInstructionsByInstitution_ShortName(
          institutionToUpdate.getShortName());
    }
    return dbInstitution;
  }

  @Override
  public boolean validateAffiliation(
      @Nullable DbVerifiedInstitutionalAffiliation dbAffiliation, String contactEmail) {
    if (dbAffiliation == null) {
      return false;
    }
    return validateInstitutionalEmail(
        institutionMapper.dbToModel(dbAffiliation.getInstitution()), contactEmail);
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
  public Optional<String> getInstitutionUserInstructions(final String shortName) {
    return institutionUserInstructionsDao
        .getByInstitution(getDbInstitutionOrThrow(shortName))
        .map(DbInstitutionUserInstructions::getUserInstructions);
  }

  @Override
  public void setInstitutionUserInstructions(final InstitutionUserInstructions userInstructions) {

    final DbInstitutionUserInstructions dbInstructions =
        institutionUserInstructionsMapper.modelToDb(userInstructions, this);

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
}
