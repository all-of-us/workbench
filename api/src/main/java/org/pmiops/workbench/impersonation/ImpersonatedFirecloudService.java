package org.pmiops.workbench.impersonation;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;

/**
 * * An impersonation-enabled version of {@link org.pmiops.workbench.firecloud.FireCloudService}
 *
 * <p>REMINDER: With great power comes great responsibility. Impersonation should not be used in
 * production, except where absolutely necessary.
 */
public interface ImpersonatedFirecloudService {

  List<RawlsWorkspaceListResponse> getWorkspaces(@Nonnull DbUser dbUser) throws IOException;

  void deleteWorkspace(@Nonnull DbUser dbUser, String workspaceNamespace, String firecloudName)
      throws IOException;

  /** Deletes SAM Kubernetes within a Google project. */
  void deleteSamKubernetesResourcesInWorkspace(@Nonnull DbUser dbUser, String googleProjectId)
      throws IOException;
}
