package org.pmiops.workbench.iam;

import java.util.List;

public interface IamService {
  /**
   * Revokes permissions to run Google Life Sciences jobs for the provided user email lists.
   *
   * <p>For now just revoke lifesciences.workflowsRunner permission but keep petSA ActAS permission.
   *
   * @return the list of users whose workflow runner roles we failed to revoke, if any
   */
  List<String> revokeWorkflowRunnerRoleForUsers(String googleProject, List<String> userEmails);
}
