package org.pmiops.workbench.impersonation;

import static org.pmiops.workbench.firecloud.FireCloudServiceImpl.FIRECLOUD_WORKSPACE_REQUIRED_FIELDS;
import static org.pmiops.workbench.firecloud.FireCloudServiceImpl.TERMS_OF_SERVICE_BODY;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.firecloud.FirecloudApiClientFactory;
import org.pmiops.workbench.firecloud.FirecloudRetryHandler;
import org.pmiops.workbench.firecloud.api.TermsOfServiceApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.iam.SamApiClientFactory;
import org.pmiops.workbench.iam.SamRetryHandler;
import org.pmiops.workbench.sam.api.ResourcesApi;
import org.pmiops.workbench.sam.model.SamFullyQualifiedResourceId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * An impersonation-enabled version to call Firecloud services.
 *
 * <p>REMINDER: With great power comes great responsibility. Impersonation should not be used in
 * production, except where absolutely necessary.
 */
@Service
public class ImpersonatedFirecloudServiceImpl implements ImpersonatedFirecloudService {

  // https://github.com/broadinstitute/sam/blob/30931bde56a6ffcea2040086503ade37378dfffc/src/main/resources/reference.conf#L559
  private static final String SAM_GOOGLE_PROJECT_RESOURCE_NAME = "google-project";
  // https://github.com/broadinstitute/sam/blob/30931bde56a6ffcea2040086503ade37378dfffc/src/main/resources/reference.conf#L782
  private static final String SAM_KUBERNETES_RESOURCE_NAME = "kubernetes-app";
  private final FirecloudApiClientFactory firecloudApiClientFactory;
  private final SamApiClientFactory samApiClientFactory;
  private final FirecloudRetryHandler firecloudRetryHandler;
  private final SamRetryHandler samRetryHandler;

  @Autowired
  public ImpersonatedFirecloudServiceImpl(
      FirecloudApiClientFactory firecloudApiClientFactory,
      SamApiClientFactory samApiClientFactory,
      FirecloudRetryHandler fire,
      SamRetryHandler samRetryHandler) {
    this.firecloudApiClientFactory = firecloudApiClientFactory;
    this.samApiClientFactory = samApiClientFactory;
    this.firecloudRetryHandler = fire;
    this.samRetryHandler = samRetryHandler;
  }

  @Override
  public void acceptTermsOfService(@Nonnull DbUser dbUser) throws IOException {
    TermsOfServiceApi termsOfServiceApi = getImpersonatedTosApi(dbUser);
    firecloudRetryHandler.run(
        (context) -> termsOfServiceApi.acceptTermsOfService(TERMS_OF_SERVICE_BODY));
  }

  @Override
  public boolean getUserTermsOfServiceStatus(@Nonnull DbUser dbUser) throws IOException {
    TermsOfServiceApi termsOfServiceApi = getImpersonatedTosApi(dbUser);
    return firecloudRetryHandler.run((context) -> termsOfServiceApi.getTermsOfServiceStatus());
  }

  @Override
  public List<FirecloudWorkspaceResponse> getWorkspaces(@Nonnull DbUser dbUser) throws IOException {
    WorkspacesApi workspacesApi = getImpersonatedWorkspacesApi(dbUser);
    return firecloudRetryHandler.run(
        (context) -> workspacesApi.listWorkspaces(FIRECLOUD_WORKSPACE_REQUIRED_FIELDS));
  }

  @Override
  public void deleteSamKubernetesResourceInWorkspace(@Nonnull DbUser dbUser, String googleProjectId)
      throws IOException {
    ResourcesApi resourcesApi = getImpersonatedResourceApi(dbUser);
    List<SamFullyQualifiedResourceId> resourceIds =
        samRetryHandler.run(
            (context) ->
                resourcesApi.listResourceChildren(
                    SAM_GOOGLE_PROJECT_RESOURCE_NAME, googleProjectId));
    resourceIds.stream()
        .filter(r -> r.getResourceTypeName().equals(SAM_KUBERNETES_RESOURCE_NAME))
        .forEach(
            r ->
                samRetryHandler.run(
                    (context) -> {
                      resourcesApi.deleteResourceV2(SAM_KUBERNETES_RESOURCE_NAME, r);
                      return null;
                    }));
  }

  @Override
  public void deleteWorkspace(
      @Nonnull DbUser dbUser, String workspaceNamespace, String firecloudName) throws IOException {
    WorkspacesApi workspacesApi = getImpersonatedWorkspacesApi(dbUser);
    firecloudRetryHandler.run(
        (context) -> {
          workspacesApi.deleteWorkspace(workspaceNamespace, firecloudName);
          return null;
        });
  }

  private TermsOfServiceApi getImpersonatedTosApi(@Nonnull DbUser dbUser) throws IOException {
    return new TermsOfServiceApi(
        firecloudApiClientFactory.newImpersonatedApiClient(dbUser.getUsername()));
  }

  private WorkspacesApi getImpersonatedWorkspacesApi(@Nonnull DbUser dbUser) throws IOException {
    return new WorkspacesApi(
        firecloudApiClientFactory.newImpersonatedApiClient(dbUser.getUsername()));
  }

  private ResourcesApi getImpersonatedResourceApi(@Nonnull DbUser dbUser) throws IOException {
    return new ResourcesApi(samApiClientFactory.newImpersonatedApiClient(dbUser.getUsername()));
  }
}
