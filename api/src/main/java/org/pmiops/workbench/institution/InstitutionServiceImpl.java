package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
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

  private final Provider<InstitutionDao> institutionDaoProvider;
  private final Provider<InstitutionEmailDomainDao> institutionEmailDomainDaoProvider;
  private final Provider<InstitutionEmailAddressDao> institutionEmailAddressDaoProvider;

  @Autowired
  InstitutionServiceImpl(
      Provider<InstitutionDao> institutionDaoProvider,
      Provider<InstitutionEmailDomainDao> institutionEmailDomainDaoProvider,
      Provider<InstitutionEmailAddressDao> institutionEmailAddressDaoProvider) {
    this.institutionDaoProvider = institutionDaoProvider;
    this.institutionEmailDomainDaoProvider = institutionEmailDomainDaoProvider;
    this.institutionEmailAddressDaoProvider = institutionEmailAddressDaoProvider;
  }

  @Override
  public List<Institution> getInstitutions() {
    return StreamSupport.stream(institutionDaoProvider.get().findAll().spliterator(), false)
        .map(this::toModelClass)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Institution> getInstitution(final String id) {
    return getDbInstitution(id).map(this::toModelClass);
  }

  @Override
  public Institution createInstitution(final Institution institutionToCreate) {
    return toModelClass(saveInstitution(institutionToCreate, new DbInstitution()));
  }

  @Override
  public boolean deleteInstitution(final String id) {
    Optional<DbInstitution> dbInst = getDbInstitution(id);
    if (dbInst.isPresent()) {
      institutionDaoProvider.get().delete(dbInst.get());
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Optional<Institution> updateInstitution(
      final String id, final Institution institutionToUpdate) {
    return getDbInstitution(id)
        .map(dbInst -> toModelClass(saveInstitution(institutionToUpdate, dbInst)));
  }

  private Optional<DbInstitution> getDbInstitution(String id) {
    return institutionDaoProvider.get().findOneByShortName(id);
  }

  private DbInstitution saveInstitution(final Institution modelClass, final DbInstitution dbClass) {
    dbClass.setShortName(modelClass.getShortName());
    dbClass.setDisplayName(modelClass.getDisplayName());
    dbClass.setOrganizationTypeEnum(
        DbStorageEnums.organizationTypeToStorage(modelClass.getOrganizationTypeEnum()));
    dbClass.setOrganizationTypeOtherText(modelClass.getOrganizationTypeOtherText());

    // save so the domain and address DAOs have something to reference
    institutionDaoProvider.get().save(dbClass);

    final InstitutionEmailDomainDao domainDao = institutionEmailDomainDaoProvider.get();
    domainDao.deleteAllByInstitution(dbClass);

    Optional.ofNullable(modelClass.getEmailDomains())
        .ifPresent(
            domains -> {
              Set<DbInstitutionEmailDomain> dbDomains =
                  domains.stream()
                      .map(domain -> new DbInstitutionEmailDomain(dbClass, domain))
                      .collect(Collectors.toSet());
              domainDao.save(dbDomains);
              dbClass.setEmailDomains(dbDomains);
            });

    final InstitutionEmailAddressDao addrDao = institutionEmailAddressDaoProvider.get();
    addrDao.deleteAllByInstitution(dbClass);

    Optional.ofNullable(modelClass.getEmailAddresses())
        .ifPresent(
            addresses -> {
              Set<DbInstitutionEmailAddress> dbAddrs =
                  addresses.stream()
                      .map(address -> new DbInstitutionEmailAddress(dbClass, address))
                      .collect(Collectors.toSet());
              addrDao.save(dbAddrs);
              dbClass.setEmailAddresses(dbAddrs);
            });

    return institutionDaoProvider.get().save(dbClass);
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
            domains -> {
              institution.emailDomains(
                  domains.stream()
                      .map(DbInstitutionEmailDomain::getEmailDomain)
                      .collect(Collectors.toList()));
            });

    Optional.ofNullable(dbClass.getEmailAddresses())
        .ifPresent(
            addresses -> {
              institution.emailAddresses(
                  addresses.stream()
                      .map(DbInstitutionEmailAddress::getEmailAddress)
                      .collect(Collectors.toList()));
            });

    return institution;
  }
}
