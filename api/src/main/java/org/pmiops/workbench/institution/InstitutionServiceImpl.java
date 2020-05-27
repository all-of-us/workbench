package org.pmiops.workbench.institution;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.db.model.DbInstitutionUserInstructions;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.DuaType;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionUserInstructions;
import org.pmiops.workbench.model.PublicInstitutionDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionServiceImpl implements InstitutionService {

  private static final Logger log = Logger.getLogger(InstitutionServiceImpl.class.getName());

  private final String OPERATIONAL_USER_INSTITUTION_SHORT_NAME = "AouOps";

  private final InstitutionDao institutionDao;
  private final InstitutionEmailDomainDao institutionEmailDomainDao;
  private final InstitutionEmailAddressDao institutionEmailAddressDao;
  private final InstitutionUserInstructionsDao institutionUserInstructionsDao;
  private final VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  private final InstitutionMapper institutionMapper;
  private final InstitutionEmailDomainMapper institutionEmailDomainMapper;
  private final InstitutionEmailAddressMapper institutionEmailAddressMapper;
  private final InstitutionUserInstructionsMapper institutionUserInstructionsMapper;
  private final PublicInstitutionDetailsMapper publicInstitutionDetailsMapper;

  @Autowired
  InstitutionServiceImpl(
      InstitutionDao institutionDao,
      InstitutionEmailDomainDao institutionEmailDomainDao,
      InstitutionEmailAddressDao institutionEmailAddressDao,
      InstitutionUserInstructionsDao institutionUserInstructionsDao,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao,
      InstitutionMapper institutionMapper,
      InstitutionEmailDomainMapper institutionEmailDomainMapper,
      InstitutionEmailAddressMapper institutionEmailAddressMapper,
      InstitutionUserInstructionsMapper institutionUserInstructionsMapper,
      PublicInstitutionDetailsMapper publicInstitutionDetailsMapper) {
    this.institutionDao = institutionDao;
    this.institutionEmailDomainDao = institutionEmailDomainDao;
    this.institutionEmailAddressDao = institutionEmailAddressDao;
    this.institutionUserInstructionsDao = institutionUserInstructionsDao;
    this.verifiedInstitutionalAffiliationDao = verifiedInstitutionalAffiliationDao;
    this.institutionMapper = institutionMapper;
    this.institutionEmailDomainMapper = institutionEmailDomainMapper;
    this.institutionEmailAddressMapper = institutionEmailAddressMapper;
    this.institutionUserInstructionsMapper = institutionUserInstructionsMapper;
    this.publicInstitutionDetailsMapper = publicInstitutionDetailsMapper;
  }

  @Override
  public List<Institution> getInstitutions() {
    return StreamSupport.stream(institutionDao.findAll().spliterator(), false)
        .map(institution -> institutionMapper.dbToModel(institution, this))
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
    return getDbInstitution(shortName)
        .map(institution -> institutionMapper.dbToModel(institution, this));
  }

  @Override
  public DbInstitution getDbInstitutionOrThrow(final String shortName) {
    return getDbInstitution(shortName)
        .orElseThrow(
            () ->
                new NotFoundException(String.format("Could not find Institution '%s'", shortName)));
  }

  private Optional<DbInstitution> getDbInstitution(final String shortName) {
    return institutionDao.findOneByShortName(shortName);
  }

  @Override
  public Institution createInstitution(final Institution institutionToCreate) {
    return institutionMapper.dbToModel(
        institutionDao.save(institutionMapper.modelToDb(institutionToCreate, this)), this);
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
    return getDbInstitution(shortName)
        .map(DbInstitution::getInstitutionId)
        .map(
            dbId -> {
              // create new DB object, but mark it with the original's ID to indicate that this is
              // an update
              final DbInstitution dbObjectToUpdate =
                  institutionMapper.modelToDb(institutionToUpdate, this).setInstitutionId(dbId);
              return institutionMapper.dbToModel(institutionDao.save(dbObjectToUpdate), this);
            });
  }

  @Override
  public boolean validateAffiliation(
      @Nullable DbVerifiedInstitutionalAffiliation dbAffiliation, String contactEmail) {
    if (dbAffiliation == null) {
      return false;
    }
    return validateInstitutionalEmail(
        institutionMapper.dbToModel(dbAffiliation.getInstitution(), this), contactEmail);
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
  public List<String> getInstitutionEmailDomains(String institutionShortName) {
    return institutionEmailDomainDao
        .getByInstitutionId(getDbInstitutionOrThrow(institutionShortName).getInstitutionId())
        .stream()
        .map(DbInstitutionEmailDomain::getEmailDomain)
        .sorted()
        .distinct()
        .collect(Collectors.toList());
  }

  @Override
  public void setInstitutionEmailDomains(Institution institution) {
    final DbInstitution dbInstitution = getDbInstitutionOrThrow(institution.getShortName());
    institutionEmailDomainDao.deleteByInstitutionId(dbInstitution.getInstitutionId());

    Optional.ofNullable(institution.getEmailAddresses()).orElse(Collections.emptyList()).stream()
        .distinct()
        .map(
            address ->
                institutionEmailDomainMapper.modelToDb(address, dbInstitution.getInstitutionId()))
        .forEach(institutionEmailDomainDao::save);
  }

  @Override
  public List<String> getInstitutionEmailAddresses(String institutionShortName) {
    return institutionEmailAddressDao
        .getByInstitutionId(getDbInstitutionOrThrow(institutionShortName).getInstitutionId())
        .stream()
        .map(DbInstitutionEmailAddress::getEmailAddress)
        .sorted()
        .distinct()
        .collect(Collectors.toList());
  }

  @Override
  public void setInstitutionEmailAddresses(Institution institution) {
    final DbInstitution dbInstitution = getDbInstitutionOrThrow(institution.getShortName());
    institutionEmailAddressDao.deleteByInstitutionId(dbInstitution.getInstitutionId());

    Optional.ofNullable(institution.getEmailAddresses()).orElse(Collections.emptyList()).stream()
        .distinct()
        .map(
            address ->
                institutionEmailAddressMapper.modelToDb(address, dbInstitution.getInstitutionId()))
        .forEach(institutionEmailAddressDao::save);
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
