package org.pmiops.workbench.api;

import org.joda.time.DateTime;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class OfflineWorkspaceController implements OfflineWorkspaceApiDelegate{

  private final WorkspaceDao workspaceDao;

  @Autowired
  OfflineWorkspaceController(WorkspaceDao workspaceDao) {
    this.workspaceDao = workspaceDao;
  }

  @Override
  public ResponseEntity<Void> updateResearchPurposeReview() {
    DateTime currentDate = new DateTime();
    List<DbWorkspace> workspaceList = workspaceDao.findAllByReviewResearchPurpose((short)1);

    // 1. Filter out workspace where creation date is 365 days or 17 days
    // 2. set Review Requested to FALSE
    // 3. Save
    workspaceList = workspaceList.stream()
      .filter(workspace -> {
        DateTime workspaceCreationDate = new DateTime(workspace.getCreationTime());
        return checkReviewDate(workspaceCreationDate, currentDate);
      }).map(filteredWorkspace -> {
        filteredWorkspace.setReviewRequested(false);
        return filteredWorkspace;
      }).collect(Collectors.toList());
    workspaceDao.save(workspaceList);
    return null;
  }

  private boolean checkReviewDate(DateTime workspaceCreationDate, DateTime currentDate) {
    DateTime creationDatePlus15 = workspaceCreationDate.plusDays(15);
    return currentDate.getDayOfYear() == workspaceCreationDate.getDayOfYear()
        || currentDate.getDayOfYear() == creationDatePlus15.getDayOfYear();
  }
}

