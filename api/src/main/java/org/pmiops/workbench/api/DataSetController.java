package org.pmiops.workbench.api;

import com.google.common.base.Strings;
import org.pmiops.workbench.db.dao.DataSetService;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.DataSetValues;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.model.DataSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Provider;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;


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
      throw new BadRequestException("Invslid Request: missing name");
    } else if (dataSetRequest.getConceptSetIds() == null || dataSetRequest.getConceptSetIds().size() == 0) {
      throw new BadRequestException("Invslid Request: missing concept set ids");
    } else if (dataSetRequest.getCohortIds() == null || dataSetRequest.getCohortIds().size() == 0) {
      throw new BadRequestException("Invalid Request: missing cohort ids");
    } else if (dataSetRequest.getValues() == null || dataSetRequest.getValues().size() == 0) {
      throw new BadRequestException("Invalid Request: missing values");
    }
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    long wId = workspaceService.get(workspaceNamespace, workspaceId).getWorkspaceId();
    List<DataSetValues> dataSetValuesList = new ArrayList();
    dataSetRequest.getValues().forEach((domainValueSet) -> {
      domainValueSet.getValues().forEach((value) -> {
        dataSetValuesList.add(
            new DataSetValues(domainValueSet.getDomain().name(), value.getValue()));
      });
    });
   try {
     org.pmiops.workbench.db.model.DataSet savedDataSet = dataSetService.saveDataSet(
         dataSetRequest.getName(), dataSetRequest.getDescription(), wId, dataSetRequest.getCohortIds(),
         dataSetRequest.getConceptSetIds(), dataSetValuesList, userProvider.get().getUserId(),
         now);
   } catch (ObjectOptimisticLockingFailureException e ) {
     throw new ConflictException("Data set with same name already exist");
   }
    return null;
  }



}
