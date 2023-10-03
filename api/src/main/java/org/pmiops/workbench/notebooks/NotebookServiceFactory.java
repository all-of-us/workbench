package org.pmiops.workbench.notebooks;

import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class NotebookServiceFactory {

  private final NotebooksService awsNotebookService;
  private final NotebooksService gcpNotebookService;

  public NotebookServiceFactory(
      @Qualifier("awsNotebookService") NotebooksService awsNotebookService,
      NotebooksService gcpNotebookService) {
    this.awsNotebookService = awsNotebookService;
    this.gcpNotebookService = gcpNotebookService;
  }

  public NotebooksService getNotebookService(DbWorkspace workspace) {
    if (workspace.isAws()) return awsNotebookService;
    else return gcpNotebookService;
  }
}
