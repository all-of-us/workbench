package org.pmiops.workbench.institution;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.model.Institution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InstitutionServiceImpl implements InstitutionService {

  private final InstitutionDao institutionDao;

  @Autowired
  InstitutionServiceImpl(InstitutionDao institutionDao) {
    this.institutionDao = institutionDao;
  }

  @Override
  public List<Institution> getInstitutions() {
    return StreamSupport.stream(institutionDao.findAll().spliterator(), false)
        .map(this::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Institution> getInstitution(final String shortName) {
    return getDbInstitution(shortName).map(this::toModel);
  }

  @Override
  public Institution createInstitution(final Institution institutionToCreate) {
    return toModel(institutionDao.save(newDbObject(institutionToCreate)));
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
        .map(dbInst -> toModel(institutionDao.save(updateDbObject(dbInst, institutionToUpdate))));
  }

  private Optional<DbInstitution> getDbInstitution(String shortName) {
    return institutionDao.findOneByShortName(shortName);
  }

  private DbInstitution newDbObject(final Institution modelObject) {
    return updateDbObject(new DbInstitution(), modelObject);
  }

  private DbInstitution updateDbObject(
      final DbInstitution dbObject, final Institution modelObject) {
    return dbObject
        .shortName(modelObject.getShortName())
        .displayName(modelObject.getDisplayName())
        .organizationTypeEnum(modelObject.getOrganizationTypeEnum())
        .organizationTypeOtherText(modelObject.getOrganizationTypeOtherText())
        .emailDomains(
            Optional.ofNullable(modelObject.getEmailDomains()).orElse(Collections.emptyList())
                .stream()
                .map(domain -> new DbInstitutionEmailDomain().setEmailDomain(domain))
                .collect(Collectors.toSet()))
        .emailAddresses(
            Optional.ofNullable(modelObject.getEmailAddresses()).orElse(Collections.emptyList())
                .stream()
                .map(address -> new DbInstitutionEmailAddress().setEmailAddress(address))
                .collect(Collectors.toSet()));
  }

  private Institution toModel(final DbInstitution dbObject) {
    return new Institution()
        .shortName(dbObject.getShortName())
        .displayName(dbObject.getDisplayName())
        .organizationTypeEnum(dbObject.getOrganizationTypeEnum())
        .organizationTypeOtherText(dbObject.getOrganizationTypeOtherText())
        .emailDomains(
            dbObject.getEmailDomains().stream()
                .map(DbInstitutionEmailDomain::getEmailDomain)
                .collect(Collectors.toList()))
        .emailAddresses(
            dbObject.getEmailAddresses().stream()
                .map(DbInstitutionEmailAddress::getEmailAddress)
                .collect(Collectors.toList()));
  }
}
