package org.pmiops.workbench.api;

import java.util.List;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineWorkspaceController implements OfflineWorkspaceApiDelegate {

  private final WorkspaceDao workspaceDao;

  @Autowired
  OfflineWorkspaceController(WorkspaceDao workspaceDao) {
    this.workspaceDao = workspaceDao;
  }

  @Override
  public ResponseEntity<Void> updateResearchPurposeReview() {
    List<DbWorkspace> workspaceList = workspaceDao.findAllByReviewResearchPurpose((short) 1);

    // 1. Filter out workspace where creation date is 365 days or 17 days
    // 2. set Review Requested to FALSE
    // 3. Update workspace
    workspaceList =
        workspaceList.stream()
            .filter(
                workspace -> {
                  DateTime workspaceCreationDate = new DateTime(workspace.getCreationTime());
                  DateTime workspaceModifiedDate = new DateTime(workspace.getLastModifiedTime());
                  return checkReviewDate(workspaceCreationDate, workspaceModifiedDate);
                })
            .map(
                filteredWorkspace -> {
                  filteredWorkspace.setReviewRequested(false);
                  return filteredWorkspace;
                })
            .collect(Collectors.toList());
    workspaceDao.save(workspaceList);
    return null;
  }

  private boolean checkReviewDate(DateTime workspaceCreationDate, DateTime workspaceModifiedDate) {
    DateTime creationDatePlus15 = workspaceCreationDate.plusDays(15);
    DateTime creationDatePlus1Year = workspaceCreationDate.plusYears(1);
    // True if
    // 1. Current date is creation Date + 1 year or
    // 2. Current date is 15 days after creation date or
    // 3. Current Date is before current date (in case cron job did not run) and the Workspace has
    // not been modified after 1 year of creation date.
    return (creationDatePlus1Year.isEqualNow()
        || creationDatePlus15.isEqualNow()
        || creationDatePlus1Year.isBeforeNow()
            && workspaceModifiedDate.isBefore(creationDatePlus1Year));
  }
}
