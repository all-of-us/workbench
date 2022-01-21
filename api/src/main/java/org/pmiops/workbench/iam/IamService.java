package org.pmiops.workbench.iam;

import java.util.List;

public interface IamService {
  /**
   * Grants user permissions to run Google lifescience jobs to user in currenet context.
   *
   * <p>The users's Terra PET service account will get: lifescienceRunner and serviceAccountUser(on
   * the petSA itself).
   */
  void grantWorkflowRunnerRoleToCurrentUser(String googleProject);
  /**
   * Grants and revokes permissions to run Google lifescience jobs for the provideded user email
   * lists.
   *
   * <p>The users's Terra PET service account will get: lifescienceRunner and serviceAccountUser(on
   * the petSA itself).
   *
   * <p>For now just revoke lifescience runner permisison but keep petSA ActAS permission.
   */
  void updateWorkflowRunnerRoleToUsers(
      String googleProject, List<String> userEmailsGainAccess, List<String> userEmailsLostAccess);
}
