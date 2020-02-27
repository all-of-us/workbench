package org.pmiops.workbench.cohortbuilder;

import java.util.List;
import java.util.stream.Collectors;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.dao.PersonDao;
import org.pmiops.workbench.cohortbuilder.mappers.AgeTypeCountMapper;
import org.pmiops.workbench.model.AgeTypeCount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortBuilderServiceImpl implements CohortBuilderService {

  private CdrVersionService cdrVersionService;
  private PersonDao personDao;
  private AgeTypeCountMapper ageTypeCountMapper;

  @Autowired
  public CohortBuilderServiceImpl(
      CdrVersionService cdrVersionService,
      PersonDao personDao,
      AgeTypeCountMapper ageTypeCountMapper) {
    this.cdrVersionService = cdrVersionService;
    this.personDao = personDao;
    this.ageTypeCountMapper = ageTypeCountMapper;
  }

  @Override
  public List<AgeTypeCount> findAgeTypeCounts(Long cdrVersionId) {
    this.cdrVersionService.setCdrVersion(cdrVersionId);
    return personDao.findAgeTypeCounts().stream()
        .map(ageTypeCountMapper::dbModelToClient)
        .collect(Collectors.toList());
  }
}
