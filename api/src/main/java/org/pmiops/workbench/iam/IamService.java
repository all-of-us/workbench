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
   * Grants permissions to run Google lifescience jobs for the provideded user email lists.
   *
   * <p>The users's Terra PET service account will get: lifescienceRunner and serviceAccountUser(on
   * the petSA itself).
   */
  void grantWorkflowRunnerRoleToUsers(String googleProject, List<String> userEmails);
}
