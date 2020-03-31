package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.jetbrains.annotations.Nullable;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AgreementType;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.PublicInstitutionDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InstitutionServiceImpl implements InstitutionService {

  private final InstitutionDao institutionDao;
  private final InstitutionMapper institutionMapper;
  private final PublicInstitutionDetailsMapper publicInstitutionDetailsMapper;
  private final VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  @Autowired
  InstitutionServiceImpl(
      InstitutionDao institutionDao,
      InstitutionMapper institutionMapper,
      PublicInstitutionDetailsMapper publicInstitutionDetailsMapper,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao) {
    this.institutionDao = institutionDao;
    this.institutionMapper = institutionMapper;
    this.publicInstitutionDetailsMapper = publicInstitutionDetailsMapper;
    this.verifiedInstitutionalAffiliationDao = verifiedInstitutionalAffiliationDao;
  }

  @Override
  public List<Institution> getInstitutions() {
    return StreamSupport.stream(institutionDao.findAll().spliterator(), false)
        .map(institutionMapper::dbToModel)
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
    return getDbInstitution(shortName).map(institutionMapper::dbToModel);
  }

  @Override
  public Optional<DbInstitution> getDbInstitution(final String shortName) {
    return institutionDao.findOneByShortName(shortName);
  }

  @Override
  public Institution createInstitution(final Institution institutionToCreate) {
    return institutionMapper.dbToModel(
        institutionDao.save(institutionMapper.modelToDb(institutionToCreate)));
  }

  @Override
  public void deleteInstitution(final String shortName) {
    final DbInstitution institution =
        getDbInstitution(shortName)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format(
                            "Could not delete Institution '%s' because it was not found",
                            shortName)));

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
                  institutionMapper.modelToDb(institutionToUpdate).setInstitutionId(dbId);
              return institutionMapper.dbToModel(institutionDao.save(dbObjectToUpdate));
            });
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
    } catch (AddressException | NullPointerException e) {
      return false;
    }

    // If the Institution has DUA Agreement that is restricted just to few employees
    // Confirm if the email address is in the allowed email list
    if (institution.getAgreementTypeEnum() != null
        && institution.getAgreementTypeEnum().equals(AgreementType.RESTRICTED)) {
      return institution.getEmailAddresses().contains(contactEmail);
    }

    // If Agreement Type is NULL assume DUA Agreement is MASTER
    // If Institution agreement type is master confirm if the contact email has valid/allowed domain
    final String contactEmailDomain = contactEmail.substring(contactEmail.indexOf("@") + 1);
    return institution.getEmailDomains().contains(contactEmailDomain);
  }
}
