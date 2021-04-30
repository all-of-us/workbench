package org.pmiops.workbench.cohorts;

import java.util.List;
import java.util.stream.Collectors;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.model.Cohort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortService {

  private final BigQueryService bigQueryService;
  private final CohortQueryBuilder cohortQueryBuilder;
  private final CohortDao cohortDao;
  private final CohortMapper cohortMapper;

  @Autowired
  public CohortService(
      BigQueryService bigQueryService,
      CohortQueryBuilder cohortQueryBuilder,
      CohortDao cohortDao,
      CohortMapper cohortMapper) {
    this.bigQueryService = bigQueryService;
    this.cohortQueryBuilder = cohortQueryBuilder;
    this.cohortDao = cohortDao;
    this.cohortMapper = cohortMapper;
  }

<<<<<<< HEAD
  public List<String> getPersonIdsWithWholeGenome(Long cohortId) {
    String cohortDefinition = cohortDao.findById(cohortId).get().getCriteria();

    // AND the existing search criteria with participants having genomics data.
    final SearchRequest searchRequest = new Gson().fromJson(cohortDefinition, SearchRequest.class);
    searchRequest.addIncludesItem(
        new SearchGroup()
            .items(
                ImmutableList.of(
                    new SearchGroupItem()
                        .type(Domain.WHOLE_GENOME_VARIANT.toString())
                        .addSearchParametersItem(
                            new SearchParameter()
                                .domain(Domain.WHOLE_GENOME_VARIANT.toString())
                                .type(CriteriaType.PPI.toString())
                                .group(false)))));
    final QueryJobConfiguration participantIdQuery =
        cohortQueryBuilder.buildParticipantIdQuery(new ParticipantCriteria(searchRequest));

    return Streams.stream(
            bigQueryService
                .executeQuery(bigQueryService.filterBigQueryConfig(participantIdQuery))
                .getValues())
        .map(personId -> personId.get(0).getValue().toString())
        .collect(Collectors.toList());
  }

=======
>>>>>>> origin/master
  public List<Cohort> findAll(List<Long> cohortIds) {
    return ((List<DbCohort>) cohortDao.findAllById(cohortIds))
        .stream().map(cohortMapper::dbModelToClient).collect(Collectors.toList());
  }
}
