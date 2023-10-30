package org.pmiops.workbench.notebooks;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
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
        ImmutableMap.of(
            CloudPlatform.GCP, gcpNotebookService,
            CloudPlatform.AWS, awsNotebookService);
  }

  public NotebooksService getNotebookService(CloudPlatform cloudPlatform) {
    return notebooksServices.getOrDefault(cloudPlatform, defaultNotebookService);
  }
}
