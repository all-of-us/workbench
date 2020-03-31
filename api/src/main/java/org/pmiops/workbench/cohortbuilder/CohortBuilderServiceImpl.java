package org.pmiops.workbench.cohortbuilder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.CBDataFilterDao;
import org.pmiops.workbench.cdr.dao.PersonDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cohortbuilder.mappers.AgeTypeCountMapper;
import org.pmiops.workbench.cohortbuilder.mappers.CriteriaMapper;
import org.pmiops.workbench.cohortbuilder.mappers.DataFilterMapper;
import org.pmiops.workbench.model.AgeTypeCount;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.DataFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class CohortBuilderServiceImpl implements CohortBuilderService {

  private static final Integer DEFAULT_TREE_SEARCH_LIMIT = 100;

  private CdrVersionService cdrVersionService;
  // dao objects
  private CBCriteriaDao cbCriteriaDao;
  private CBDataFilterDao cbDataFilterDao;
  private PersonDao personDao;
  // mappers
  private AgeTypeCountMapper ageTypeCountMapper;
  private CriteriaMapper criteriaMapper;
  private DataFilterMapper dataFilterMapper;

  @Autowired
  public CohortBuilderServiceImpl(
      CdrVersionService cdrVersionService,
      CBCriteriaDao cbCriteriaDao,
      CBDataFilterDao cbDataFilterDao,
      PersonDao personDao,
      AgeTypeCountMapper ageTypeCountMapper,
      CriteriaMapper criteriaMapper,
      DataFilterMapper dataFilterMapper) {
    this.cdrVersionService = cdrVersionService;
    this.cbCriteriaDao = cbCriteriaDao;
    this.cbDataFilterDao = cbDataFilterDao;
    this.personDao = personDao;
    this.ageTypeCountMapper = ageTypeCountMapper;
    this.criteriaMapper = criteriaMapper;
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
  public List<Criteria> findCriteriaAutoComplete(
      Long cdrVersionId, String domain, String term, String type, Boolean standard, Integer limit) {
    this.cdrVersionService.setCdrVersion(cdrVersionId);
    PageRequest pageRequest =
        new PageRequest(0, Optional.ofNullable(limit).orElse(DEFAULT_TREE_SEARCH_LIMIT));
    List<DbCriteria> criteriaList =
        cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndSynonyms(
            domain, type, standard, modifyTermMatch(term), pageRequest);
    if (criteriaList.isEmpty()) {
      criteriaList =
          cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndCode(
              domain, type, standard, term, pageRequest);
    }
    return criteriaList.stream().map(criteriaMapper::dbModelToClient).collect(Collectors.toList());
  }

  @Override
  public List<DataFilter> findDataFilters(Long cdrVersionId) {
    this.cdrVersionService.setCdrVersion(cdrVersionId);
    return StreamSupport.stream(cbDataFilterDao.findAll().spliterator(), false)
        .map(dataFilterMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  private String modifyTermMatch(String term) {
    String[] keywords = term.split("\\W+");
    if (keywords.length == 1 && keywords[0].length() <= 3) {
      return "+\"" + keywords[0];
    }

    return IntStream.range(0, keywords.length)
        .filter(i -> keywords[i].length() > 2)
        .mapToObj(
            i -> {
              if ((i + 1) != keywords.length) {
                return "+\"" + keywords[i] + "\"";
              }
              return "+" + keywords[i] + "*";
            })
        .collect(Collectors.joining());
  }
}
