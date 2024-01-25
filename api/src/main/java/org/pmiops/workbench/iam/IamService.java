package org.pmiops.workbench.iam;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;

public interface IamService {

  Optional<String> getOrCreatePetServiceAccountUsingImpersonation(
      String googleProject, String userEmail) throws IOException, ApiException;

  /**
   * Revokes permissions to run Google Life Sciences jobs for the provided user email lists.
   *
   * <p>For now just revoke lifesciences.workflowsRunner permission but keep petSA ActAS permission.
   *
   * @return the list of users whose workflow runner roles we failed to revoke, if any
   */
  List<String> revokeWorkflowRunnerRoleForUsers(String googleProject, List<String> userEmails);
}
