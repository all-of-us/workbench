package org.pmiops.workbench.apiclients.leonardo;

import java.util.List;
import org.broadinstitute.dsde.workbench.client.leonardo.model.GetPersistentDiskResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.pmiops.workbench.exceptions.WorkbenchException;

public interface NewLeonardoApiClient {
  String LEONARDO_CREATOR_ROLE = "creator";

  GetPersistentDiskResponse getPersistentDisk(String googleProject, String diskName)
      throws WorkbenchException;

  void deletePersistentDisk(String googleProject, String diskName) throws WorkbenchException;

  void updatePersistentDisk(String googleProject, String diskName, Integer diskSize)
      throws WorkbenchException;

  List<ListPersistentDiskResponse> listPersistentDiskByProjectCreatedByCreator(
      String googleProject, boolean includeDeleted);
}
