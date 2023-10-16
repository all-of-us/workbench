package org.pmiops.workbench.notebooks;

import java.util.Map;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.CloudPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class NotebookServiceFactory {

  private final Map<CloudPlatform, NotebooksService> notebooksServices;
  private final NotebooksService defaultNotebookService;

  @Autowired
  public NotebookServiceFactory(
      @Qualifier("awsNotebookService") NotebooksService awsNotebookService,
      NotebooksService gcpNotebookService) {
    defaultNotebookService = gcpNotebookService;
    notebooksServices =
        Map.of(
            CloudPlatform.GCP, gcpNotebookService,
            CloudPlatform.AWS, awsNotebookService);
  }

  public NotebooksService getNotebookService(DbWorkspace workspace) {
    return notebooksServices.getOrDefault(workspace.getCloudPlatform(), defaultNotebookService);
  }
}
