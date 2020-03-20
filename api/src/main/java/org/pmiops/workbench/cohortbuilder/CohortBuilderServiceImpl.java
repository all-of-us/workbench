package org.pmiops.workbench.cohortbuilder;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.dao.CBDataFilterDao;
import org.pmiops.workbench.cdr.dao.PersonDao;
import org.pmiops.workbench.cohortbuilder.mappers.AgeTypeCountMapper;
import org.pmiops.workbench.cohortbuilder.mappers.DataFilterMapper;
import org.pmiops.workbench.model.AgeTypeCount;
import org.pmiops.workbench.model.DataFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortBuilderServiceImpl implements CohortBuilderService {

  private CdrVersionService cdrVersionService;
  private PersonDao personDao;
  private CBDataFilterDao cbDataFilterDao;
  private AgeTypeCountMapper ageTypeCountMapper;
  private DataFilterMapper dataFilterMapper;

  @Autowired
  public CohortBuilderServiceImpl(
      CdrVersionService cdrVersionService,
      PersonDao personDao,
      CBDataFilterDao cbDataFilterDao,
      AgeTypeCountMapper ageTypeCountMapper,
      DataFilterMapper dataFilterMapper) {
    this.cdrVersionService = cdrVersionService;
    this.personDao = personDao;
    this.cbDataFilterDao = cbDataFilterDao;
    this.ageTypeCountMapper = ageTypeCountMapper;
    this.dataFilterMapper = dataFilterMapper;
  }

  @Override
  public List<AgeTypeCount> findAgeTypeCounts(Long cdrVersionId) {
    this.cdrVersionService.setCdrVersion(cdrVersionId);
    return personDao.findAgeTypeCounts().stream()
        .map(ageTypeCountMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public List<DataFilter> findDataFilters(Long cdrVersionId) {
    this.cdrVersionService.setCdrVersion(cdrVersionId);
    return StreamSupport.stream(cbDataFilterDao.findAll().spliterator(), false)
        .map(dataFilterMapper::dbModelToClient)
        .collect(Collectors.toList());
  }
}
