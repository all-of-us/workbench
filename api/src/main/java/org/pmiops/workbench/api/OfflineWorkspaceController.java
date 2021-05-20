package org.pmiops.workbench.api;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineWorkspaceController implements OfflineWorkspaceApiDelegate {

  private final WorkspaceDao workspaceDao;
  private final Provider<WorkbenchConfig> configProvider;

  @Autowired
  OfflineWorkspaceController(Provider<WorkbenchConfig> configProvider, WorkspaceDao workspaceDao) {
    this.configProvider = configProvider;
    this.workspaceDao = workspaceDao;
  }

  @Override
  public ResponseEntity<Void> updateResearchPurposeReviewPrompt() {
    if (configProvider.get().featureFlags.enableResearchPurposePrompt) {
      List<DbWorkspace> workspaceList =
          workspaceDao.findAllByNeedsResearchPurposeReviewPrompt((short) 1);

      // 1. Filter out workspace where creation date is 365 days or 15 days
      // 2. set Research Purpose Review to FALSE
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
                    filteredWorkspace.setNeedsReviewPrompt(true);
                    return filteredWorkspace;
                  })
              .collect(Collectors.toList());
      workspaceDao.saveAll(workspaceList);
    }
    return ResponseEntity.noContent().build();
  }

  private boolean checkReviewDate(DateTime workspaceCreationDate, DateTime workspaceModifiedDate) {
    DateTime creationDatePlus15 = workspaceCreationDate.plusDays(15);
    DateTime creationDatePlus1Year = workspaceCreationDate.plusYears(1);

    // True if
    // 1. Current date is creation Date + 1 year or
    // 2. Current date is 15 days after creation date or
    // 3. Current Date is AFTER an year of creation date (in case cron job did not run/ missed
    // picking the workspace) and the Workspace has
    // not been modified after 1 year of creation date.
    int compareCreationDatePlus1Year =
        DateTimeComparator.getDateOnlyInstance().compare(creationDatePlus1Year, null);
    return compareCreationDatePlus1Year == 0
        || DateTimeComparator.getDateOnlyInstance().compare(creationDatePlus15, null) == 0
        || (compareCreationDatePlus1Year < 0
            && workspaceModifiedDate.isBefore(creationDatePlus1Year));
  }
}
