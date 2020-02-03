package org.pmiops.workbench.institution;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.InstitutionEmailAddressDao;
import org.pmiops.workbench.db.dao.InstitutionEmailDomainDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.exceptions.NotFoundException;
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
  public Institution getInstitution(final String id) {
    return toModelClass(getDbInstitution(id));
  }

  @Override
  public Institution createInstitution(final Institution institutionToCreate) {
    return toModelClass(saveInstitution(institutionToCreate, new DbInstitution()));
  }

  @Override
  public void deleteInstitution(final String id) {
    institutionDaoProvider.get().delete(getDbInstitution(id));
  }

  @Override
  public Institution updateInstitution(final String id, final Institution institutionToUpdate) {
    return toModelClass(saveInstitution(institutionToUpdate, getDbInstitution(id)));
  }

  @NotNull
  private DbInstitution getDbInstitution(String id) {
    final DbInstitution institution = institutionDaoProvider.get().findOneByApiId(id);
    if (institution == null) {
      throw new NotFoundException(String.format("Could not find Institution with ID %s", id));
    }
    return institution;
  }

  private DbInstitution saveInstitution(final Institution modelClass, final DbInstitution dbClass) {
    dbClass.setApiId(modelClass.getId());
    dbClass.setLongName(modelClass.getLongName());
    dbClass.setOrganizationTypeEnum(
        DbStorageEnums.organizationTypeToStorage(modelClass.getOrganizationTypeEnum()));
    dbClass.setOrganizationTypeOtherText(modelClass.getOrganizationTypeOtherText());

    // save so the domain and address DAOs have something to reference
    institutionDaoProvider.get().save(dbClass);

    final InstitutionEmailDomainDao domainDao = institutionEmailDomainDaoProvider.get();
    domainDao.deleteAllByInstitution(dbClass);
    final List<String> domains = modelClass.getEmailDomains();
    if (domains != null) {
      domainDao.save(
          modelClass.getEmailDomains().stream()
              .map(domain -> new DbInstitutionEmailDomain(dbClass, domain))
              .collect(Collectors.toList()));
    }

    final InstitutionEmailAddressDao addrDao = institutionEmailAddressDaoProvider.get();
    addrDao.deleteAllByInstitution(dbClass);
    final List<String> addrs = modelClass.getEmailAddresses();
    if (addrs != null) {
      addrDao.save(
          modelClass.getEmailAddresses().stream()
              .map(address -> new DbInstitutionEmailAddress(dbClass, address))
              .collect(Collectors.toList()));
    }

    return dbClass;
  }

  private Institution toModelClass(final DbInstitution dbClass) {
    final Institution institution =
        new Institution()
            .id(dbClass.getApiId())
            .longName(dbClass.getLongName())
            .organizationTypeEnum(
                DbStorageEnums.organizationTypeFromStorage(dbClass.getOrganizationTypeEnum()))
            .organizationTypeOtherText(dbClass.getOrganizationTypeOtherText());

    final List<DbInstitutionEmailDomain> domains = dbClass.getEmailDomains();
    if (domains != null) {
      institution.emailDomains(
          domains.stream()
              .map(DbInstitutionEmailDomain::getEmailDomain)
              .collect(Collectors.toList()));
    }

    final List<DbInstitutionEmailAddress> addresses = dbClass.getEmailAddresses();
    if (addresses != null) {
      institution.emailAddresses(
          addresses.stream()
              .map(DbInstitutionEmailAddress::getEmailAddress)
              .collect(Collectors.toList()));
    }

    return institution;
  }
}
