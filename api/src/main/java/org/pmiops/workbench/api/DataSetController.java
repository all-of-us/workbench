package org.pmiops.workbench.api;

import com.google.common.base.Strings;
import org.pmiops.workbench.db.dao.DataSetService;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.DataSetValues;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValuePair;
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
  DataSetController(
      DataSetService dataSetService, Provider<User> userProvider, Clock clock, WorkspaceService workspaceService
  ) {
    this.dataSetService = dataSetService;
    this.userProvider = userProvider;
    this.clock = clock;
    this.workspaceService = workspaceService;
  }


  @Override
  public ResponseEntity<DataSet> createDataSet(String workspaceNamespace, String workspaceId,
      DataSet dataSetRequest) {
    if (Strings.isNullOrEmpty(dataSetRequest.getName())) {
      throw new BadRequestException("Invalid Request: missing name");
    } else if (dataSetRequest.getConceptSetIds() == null || dataSetRequest.getConceptSetIds().size() == 0) {
      throw new BadRequestException("Invalid Request: missing concept set ids");
    } else if (dataSetRequest.getCohortIds() == null || dataSetRequest.getCohortIds().size() == 0) {
      throw new BadRequestException("Invalid Request: missing cohort ids");
    } else if (dataSetRequest.getValues() == null || dataSetRequest.getValues().size() == 0) {
      throw new BadRequestException("Invalid Request: missing values");
    }
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    long wId = workspaceService.get(workspaceNamespace, workspaceId).getWorkspaceId();
    List<DataSetValues> dataSetValuesList = dataSetRequest.getValues().stream().map(
        (domainValueSet) -> {
          return new DataSetValues(domainValueSet.getDomain().name(), domainValueSet.getValue());
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

  private static final Function<org.pmiops.workbench.db.model.DataSet, DataSet> TO_CLIENT_DATA_SET =
      new Function<org.pmiops.workbench.db.model.DataSet, DataSet>() {
        @Override
        public DataSet apply(org.pmiops.workbench.db.model.DataSet dataSet) {
          DataSet result = new DataSet();
          result.setName(dataSet.getName());
          result.setCohortIds(dataSet.getCohortSetId());
          result.setConceptSetIds(dataSet.getConceptSetId());
          result.setDescription(dataSet.getDescription());
          List<DomainValuePair> domainValuePairList = dataSet.getValues().stream().map(
              (dataSetValue) -> {
                DomainValuePair domainValuePair = new DomainValuePair();
                domainValuePair.setValue(dataSetValue.getValue());
                domainValuePair.setDomain(Domain.valueOf(dataSetValue.getDomainId()));
                return domainValuePair;
              }).collect(Collectors.toList());
          result.setValues(domainValuePairList);
          return result;
        }
      };

}
