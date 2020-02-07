package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.InstitutionEmailAddressDao;
import org.pmiops.workbench.db.dao.InstitutionEmailDomainDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.Institution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InstitutionServiceImpl implements InstitutionService {

  private final InstitutionDao institutionDao;
  private final InstitutionEmailDomainDao institutionEmailDomainDao;
  private final InstitutionEmailAddressDao institutionEmailAddressDao;

  @Autowired
  InstitutionServiceImpl(
      InstitutionDao institutionDao,
      InstitutionEmailDomainDao institutionEmailDomainDao,
      InstitutionEmailAddressDao institutionEmailAddressDao) {
    this.institutionDao = institutionDao;
    this.institutionEmailDomainDao = institutionEmailDomainDao;
    this.institutionEmailAddressDao = institutionEmailAddressDao;
  }

  @Override
  public List<Institution> getInstitutions() {
    return StreamSupport.stream(institutionDao.findAll().spliterator(), false)
        .map(this::toModelClass)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Institution> getInstitution(final String shortName) {
    return getDbInstitution(shortName).map(this::toModelClass);
  }

  @Override
  public Institution createInstitution(final Institution institutionToCreate) {
    return toModelClass(saveInstitution(institutionToCreate, new DbInstitution()));
  }

  @Override
  public boolean deleteInstitution(final String shortName) {
    return getDbInstitution(shortName)
        .map(
            dbInst -> {
              institutionDao.delete(dbInst);
              return true;
            })
        .orElse(false);
  }

  @Override
  public Optional<Institution> updateInstitution(
      final String shortName, final Institution institutionToUpdate) {
    return getDbInstitution(shortName)
        .map(dbInst -> toModelClass(saveInstitution(institutionToUpdate, dbInst)));
  }

  private Optional<DbInstitution> getDbInstitution(String shortName) {
    return institutionDao.findOneByShortName(shortName);
  }

  private DbInstitution saveInstitution(final Institution modelClass, final DbInstitution dbClass) {
    dbClass.setShortName(modelClass.getShortName());
    dbClass.setDisplayName(modelClass.getDisplayName());
    dbClass.setOrganizationTypeEnum(
        DbStorageEnums.organizationTypeToStorage(modelClass.getOrganizationTypeEnum()));
    dbClass.setOrganizationTypeOtherText(modelClass.getOrganizationTypeOtherText());

    return saveInstitutionEmailPatterns(modelClass, institutionDao.save(dbClass));
  }

  private DbInstitution saveInstitutionEmailPatterns(
      final Institution modelClass, final DbInstitution dbClass) {

    institutionEmailDomainDao.deleteAllByInstitution(dbClass);
    Optional.ofNullable(modelClass.getEmailDomains())
        .ifPresent(
            domains ->
                dbClass.setEmailDomains(
                    institutionEmailDomainDao.save(
                        domains.stream()
                            .map(domain -> new DbInstitutionEmailDomain(dbClass, domain))
                            .collect(Collectors.toSet()))));

    institutionEmailAddressDao.deleteAllByInstitution(dbClass);
    Optional.ofNullable(modelClass.getEmailAddresses())
        .ifPresent(
            addresses ->
                dbClass.setEmailAddresses(
                    institutionEmailAddressDao.save(
                        addresses.stream()
                            .map(address -> new DbInstitutionEmailAddress(dbClass, address))
                            .collect(Collectors.toSet()))));

    return institutionDao.save(dbClass);
  }

  private Institution toModelClass(final DbInstitution dbClass) {
    final Institution institution =
        new Institution()
            .shortName(dbClass.getShortName())
            .displayName(dbClass.getDisplayName())
            .organizationTypeEnum(
                DbStorageEnums.organizationTypeFromStorage(dbClass.getOrganizationTypeEnum()))
            .organizationTypeOtherText(dbClass.getOrganizationTypeOtherText());

    Optional.ofNullable(dbClass.getEmailDomains())
        .ifPresent(
            domains ->
                institution.emailDomains(
                    domains.stream()
                        .map(DbInstitutionEmailDomain::getEmailDomain)
                        .collect(Collectors.toList())));

    Optional.ofNullable(dbClass.getEmailAddresses())
        .ifPresent(
            addresses ->
                institution.emailAddresses(
                    addresses.stream()
                        .map(DbInstitutionEmailAddress::getEmailAddress)
                        .collect(Collectors.toList())));

    return institution;
  }
}
