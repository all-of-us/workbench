package org.pmiops.workbench.impersonation;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;

/**
 * An impersonation-enabled version to call Firecloud services.
 *
 * <p>REMINDER: With great power comes great responsibility. Impersonation should not be used in
 * production, except where absolutely necessary.
 */
public interface ImpersonatedFirecloudService {
  // accept the Terra Terms of Service on behalf of a user by using impersonation
  void acceptTermsOfService(@Nonnull DbUser dbUser) throws IOException;

  // retrieve a user's Terra Terms of Service status, by using impersonation
  boolean getUserTermsOfServiceStatus(@Nonnull DbUser dbUser) throws IOException;

  List<FirecloudWorkspaceResponse> getWorkspaces(@Nonnull DbUser dbUser) throws IOException;

  void deleteWorkspace(@Nonnull DbUser dbUser, String workspaceNamespace, String firecloudName)
      throws IOException;

  /** Deletes SAM Kubernetes within a Google project. */
  void deleteSamKubernetesResourceInWorkspace(@Nonnull DbUser dbUser, String googleProjectId)
      throws IOException;
}
