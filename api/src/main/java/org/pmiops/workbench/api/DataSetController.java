package org.pmiops.workbench.api;

import com.google.common.base.Strings;
import com.google.common.collect.Streams;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetService;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.DataSetValues;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetResponse;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Provider;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


@RestController
public class DataSetController implements DataSetApiDelegate {

  private Provider<User> userProvider;
  private final WorkspaceService workspaceService;

  private DataSetService dataSetService;
  private final Clock clock;

  @Autowired
  private ConceptSetDao conceptSetDao;

  @Autowired
  private ConceptDao conceptDao;

  @Autowired
  private final CohortDao cohortDao;

  @Autowired
  DataSetController(
      DataSetService dataSetService, Provider<User> userProvider,
      Clock clock,
      WorkspaceService workspaceService,
      ConceptSetDao conceptSetDao,
      ConceptDao conceptDao,
      CohortDao cohortDao
  ) {
    this.dataSetService = dataSetService;
    this.userProvider = userProvider;
    this.clock = clock;
    this.workspaceService = workspaceService;
    this.conceptSetDao = conceptSetDao;
    this.conceptDao = conceptDao;
    this.cohortDao = cohortDao;
  }


  @Override
  public ResponseEntity<DataSetResponse> createDataSet(String workspaceNamespace, String workspaceId,
      DataSet dataSetRequest) {
    if (Strings.isNullOrEmpty(dataSetRequest.getName())) {
      throw new BadRequestException("Missing name");
    } else if (dataSetRequest.getConceptSetIds() == null || dataSetRequest.getConceptSetIds().size() == 0) {
      throw new BadRequestException("Missing concept set ids");
    } else if (dataSetRequest.getCohortIds() == null || dataSetRequest.getCohortIds().size() == 0) {
      throw new BadRequestException("Missing cohort ids");
    } else if (dataSetRequest.getValues() == null || dataSetRequest.getValues().size() == 0) {
      throw new BadRequestException("Missing values");
    }
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    workspaceService
        .getWorkspaceEnforceAccessLevelAndSetCdrVersion(workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    long wId = workspaceService.get(workspaceNamespace, workspaceId).getWorkspaceId();
    List<DataSetValues> dataSetValuesList = dataSetRequest.getValues().stream().map(
        (domainValueSet) -> {
          DataSetValues dataSetValues = new DataSetValues(domainValueSet.getDomain().name(), domainValueSet.getValue());
          dataSetValues.setDomainEnum(domainValueSet.getDomain());
          return dataSetValues;
        }).collect(Collectors.toList());
    try {
      org.pmiops.workbench.db.model.DataSet savedDataSet = dataSetService.saveDataSet(
          dataSetRequest.getName(), dataSetRequest.getDescription(), wId, dataSetRequest.getCohortIds(),
          dataSetRequest.getConceptSetIds(), dataSetValuesList, userProvider.get().getUserId(), now);
      return ResponseEntity.ok(TO_CLIENT_DATA_SET.apply(savedDataSet));
    } catch (DataIntegrityViolationException ex) {
      throw new ConflictException("Data set with the same name already exist");
    }
  }

  private final Function<org.pmiops.workbench.db.model.DataSet, DataSetResponse> TO_CLIENT_DATA_SET =
      new Function<org.pmiops.workbench.db.model.DataSet, DataSetResponse>() {
        @Override
        public DataSetResponse apply(org.pmiops.workbench.db.model.DataSet dataSet) {
          DataSetResponse result = new DataSetResponse();
          result.setName(dataSet.getName());
          Iterable<org.pmiops.workbench.db.model.ConceptSet> conceptSets =
              conceptSetDao.findAll(dataSet.getConceptSetId());
          result.setConceptSets(Streams.stream(conceptSets)
              .map(conceptSet -> toClientConceptSet(conceptSet)).collect(Collectors.toList()));

          Iterable<org.pmiops.workbench.db.model.Cohort> cohorts = cohortDao.findAll(dataSet.getCohortSetId());
          result.setCohorts(Streams.stream(cohorts)
                .map(CohortsController.TO_CLIENT_COHORT)
                .collect(Collectors.toList()));
          result.setDescription(dataSet.getDescription());
          result.setValues(dataSet.getValues()
                  .stream()
                  .map(TO_CLIENT_DOMAIN_VALUE)
                  .collect(Collectors.toList()));
          return result;
        }
      };

  private ConceptSet toClientConceptSet(org.pmiops.workbench.db.model.ConceptSet conceptSet) {
    ConceptSet result = ConceptSetsController.TO_CLIENT_CONCEPT_SET.apply(conceptSet);
    if (!conceptSet.getConceptIds().isEmpty()) {
      Iterable<org.pmiops.workbench.cdr.model.Concept> concepts =
          conceptDao.findAll(conceptSet.getConceptIds());
      result.setConcepts(Streams.stream(concepts)
         .map(ConceptsController.TO_CLIENT_CONCEPT)
          .collect(Collectors.toList()));

    }
    return result;
  }

  static final Function<DataSetValues, DomainValuePair> TO_CLIENT_DOMAIN_VALUE =
      new Function<DataSetValues, DomainValuePair>() {
        @Override
        public DomainValuePair apply(DataSetValues dataSetValue) {
          DomainValuePair domainValuePair = new DomainValuePair();
          domainValuePair.setValue(dataSetValue.getValue());
          domainValuePair.setDomain(dataSetValue.getDomainEnum());
          return  domainValuePair;
        }
      };
}
