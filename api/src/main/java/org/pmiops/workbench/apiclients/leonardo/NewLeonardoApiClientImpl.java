package org.pmiops.workbench.apiclients.leonardo;

import java.util.List;
import javax.inject.Provider;
import org.broadinstitute.dsde.workbench.client.leonardo.ApiException;
import org.broadinstitute.dsde.workbench.client.leonardo.api.DisksApi;
import org.broadinstitute.dsde.workbench.client.leonardo.model.GetPersistentDiskResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.UpdateDiskRequest;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.leonardo.LeonardoLabelHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class NewLeonardoApiClientImpl implements NewLeonardoApiClient {
  private final NewLeonardoRetryHandler leonardoRetryHandler;
  private final Provider<DisksApi> disksApiProvider;

  @Autowired
  public NewLeonardoApiClientImpl(
      NewLeonardoRetryHandler leonardoRetryHandler,
      @Qualifier(NewLeonardoConfig.USER_DISKS_API) Provider<DisksApi> disksApiProvider) {
    this.leonardoRetryHandler = leonardoRetryHandler;
    this.disksApiProvider = disksApiProvider;
  }

  @Override
  public GetPersistentDiskResponse getPersistentDisk(String googleProject, String diskName)
      throws WorkbenchException {
    DisksApi disksApi = disksApiProvider.get();
    try {
      return leonardoRetryHandler.runAndThrowChecked(
          (context) -> disksApi.getDisk(googleProject, diskName));
    } catch (ApiException e) {
      throw ExceptionUtils.newConvertLeonardoException(e);
    }
  }

  @Override
  public void deletePersistentDisk(String googleProject, String diskName)
      throws WorkbenchException {
    DisksApi disksApi = disksApiProvider.get();
    leonardoRetryHandler.run(
        (context) -> {
          disksApi.deleteDisk(googleProject, diskName);
          return null;
        });
  }

  @Override
  public void updatePersistentDisk(String googleProject, String diskName, Integer diskSize)
      throws WorkbenchException {
    DisksApi disksApi = disksApiProvider.get();
    leonardoRetryHandler.run(
        (context) -> {
          disksApi.updateDisk(googleProject, diskName, new UpdateDiskRequest().size(diskSize));
          return null;
        });
  }

  @Override
  public List<ListPersistentDiskResponse> listPersistentDiskByProjectCreatedByCreator(
      String googleProject, boolean includeDeleted) {
    DisksApi disksApi = disksApiProvider.get();
    return leonardoRetryHandler.run(
        (context) ->
            disksApi.listDisksByProject(
                googleProject,
                null,
                includeDeleted,
                LeonardoLabelHelper.LEONARDO_DISK_LABEL_KEYS,
                LEONARDO_CREATOR_ROLE));
  }
}
