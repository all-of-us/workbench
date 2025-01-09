package org.pmiops.workbench.dataset;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.Streams;
import com.google.gson.Gson;
import jakarta.inject.Provider;
import java.util.List;
import java.util.stream.Collectors;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.CohortDefinition;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.TanagraGenomicDataRequest;
import org.pmiops.workbench.tanagra.ApiException;
import org.pmiops.workbench.tanagra.api.TanagraApi;
import org.pmiops.workbench.tanagra.model.ExportPreviewRequest;
import org.springframework.stereotype.Service;

@Service
public class GenomicDatasetServiceImpl implements GenomicDatasetService {
  private final BigQueryService bigQueryService;
  private final CohortService cohortService;
  private final CohortQueryBuilder cohortQueryBuilder;
  private final Provider<TanagraApi> tanagraApiProvider;

  public GenomicDatasetServiceImpl(
      BigQueryService bigQueryService,
      CohortService cohortService,
      CohortQueryBuilder cohortQueryBuilder,
      Provider<TanagraApi> tanagraApiProvider) {
    this.bigQueryService = bigQueryService;
    this.cohortService = cohortService;
    this.cohortQueryBuilder = cohortQueryBuilder;
    this.tanagraApiProvider = tanagraApiProvider;
  }

  @Override
  public List<String> getPersonIdsWithWholeGenome(DbDataset dataSet) {
    List<ParticipantCriteria> participantCriteriaList;
    if (Boolean.TRUE.equals(dataSet.getIncludesAllParticipants())) {
      // Select all participants with WGS data.
      var cd = new CohortDefinition().addIncludesItem(createHasWgsSearchGroup());
      participantCriteriaList = List.of(new ParticipantCriteria(cd));
    } else {
      participantCriteriaList =
          cohortService.findAllByCohortIdIn(dataSet.getCohortIds()).stream()
              .map(
                  cohort -> {
                    final CohortDefinition cohortDefinition =
                        new Gson().fromJson(cohort.getCriteria(), CohortDefinition.class);
                    // AND the existing search criteria with participants having genomics data.
                    cohortDefinition.addIncludesItem(createHasWgsSearchGroup());
                    return new ParticipantCriteria(cohortDefinition);
                  })
              .toList();
    }

    final QueryJobConfiguration participantIdQuery =
        cohortQueryBuilder.buildUnionedParticipantIdQuery(participantCriteriaList);

    return Streams.stream(
            bigQueryService
                .executeQuery(bigQueryService.filterBigQueryConfig(participantIdQuery))
                .getValues())
        .map(personId -> personId.get(0).getValue().toString())
        .collect(Collectors.toList());
  }

  @Override
  public List<String> getTanagraPersonIdsWithWholeGenome(
      DbWorkspace workspace, TanagraGenomicDataRequest tanagraGenomicDataRequest) {
    List<ParticipantCriteria> participantCriteriaList;
    final QueryJobConfiguration participantIdQuery;
    if (Boolean.TRUE.equals(tanagraGenomicDataRequest.isAllParticipants())) {
      // Select all participants with WGS data.
      participantCriteriaList =
          List.of(
              new ParticipantCriteria(
                  new CohortDefinition().addIncludesItem(createHasWgsSearchGroup())));
      participantIdQuery =
          cohortQueryBuilder.buildUnionedParticipantIdQuery(participantCriteriaList);
    } else {
      try {
        DataSetRequest dataSetRequest =
            new DataSetRequest()
                .tanagraCohortIds(tanagraGenomicDataRequest.getCohortIds())
                .tanagraFeatureSetIds(tanagraGenomicDataRequest.getFeatureSetIds());
        ExportPreviewRequest exportPreviewRequest =
            createExportPreviewRequest(dataSetRequest, workspace);
        String underlayName = "aou" + workspace.getCdrVersion().getBigqueryDataset();
        String cohortsQuery =
            tanagraApiProvider
                .get()
                .describeExport(exportPreviewRequest, underlayName)
                .getEntityIdSql();
        participantIdQuery = cohortQueryBuilder.buildTanagraWGSPersonIdQuery(cohortsQuery);
      } catch (ApiException e) {
        throw new BadRequestException("Bad Request: " + e.getMessage());
      }
    }

    return Streams.stream(
            bigQueryService
                .executeQuery(bigQueryService.filterBigQueryConfig(participantIdQuery))
                .getValues())
        .map(personId -> personId.get(0).getValue().toString())
        .toList();
  }

  private SearchGroup createHasWgsSearchGroup() {
    return new SearchGroup()
        .items(
            List.of(
                new SearchGroupItem()
                    .type(Domain.WHOLE_GENOME_VARIANT.toString())
                    .addSearchParametersItem(
                        new SearchParameter()
                            .domain(Domain.WHOLE_GENOME_VARIANT.toString())
                            .type(CriteriaType.PPI.toString())
                            .group(false))));
  }

  private ExportPreviewRequest createExportPreviewRequest(
      DataSetRequest dataSetRequest, DbWorkspace dbWorkspace) {
    return new ExportPreviewRequest()
        .study(dbWorkspace.getWorkspaceNamespace())
        .cohorts(dataSetRequest.getTanagraCohortIds())
        .featureSets(dataSetRequest.getTanagraFeatureSetIds());
  }
}
