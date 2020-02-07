package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.VerifiedUserInstitutionDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.model.Institution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

@Service
@Import({InstitutionMapperImpl.class})
public class InstitutionServiceImpl implements InstitutionService {

  private final InstitutionDao institutionDao;
  private final InstitutionMapper institutionMapper;
  private final VerifiedUserInstitutionDao verifiedUserInstitutionDao;

  @Autowired
  InstitutionServiceImpl(
      InstitutionDao institutionDao,
      InstitutionMapper institutionMapper,
      VerifiedUserInstitutionDao verifiedUserInstitutionDao) {
    this.institutionDao = institutionDao;
    this.institutionMapper = institutionMapper;
    this.verifiedUserInstitutionDao = verifiedUserInstitutionDao;
  }

  @Override
  public List<Institution> getInstitutions() {
    return StreamSupport.stream(institutionDao.findAll().spliterator(), false)
        .map(institutionMapper::dbToModel)
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
  public DeletionResult deleteInstitution(final String shortName) {
    return getDbInstitution(shortName)
        .map(
            dbInst -> {
              if (verifiedUserInstitutionDao.findAllByInstitution(dbInst).isEmpty()) {
                // no verified user affiliations: safe to delete
                institutionDao.delete(dbInst);
                return DeletionResult.SUCCESS;
              } else {
                return DeletionResult.HAS_VERIFIED_AFFILIATIONS;
              }
            })
        .orElse(DeletionResult.NOT_FOUND);
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
              final DbInstitution newDbObj =
                  institutionMapper.modelToDb(institutionToUpdate).setInstitutionId(dbId);
              return institutionMapper.dbToModel(institutionDao.save(newDbObj));
            });
  }
}
