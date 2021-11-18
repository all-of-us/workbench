package org.pmiops.workbench.iam;

public interface IamClient {
  /**
   * Grants user permissions to run Google lifescience jobs.
   *
   * <p>The users's Terra PET service account will get: lifescienceRunner and serviceAccountUser(on
   * the petSA itself).
   */
  void grantWorkflowRunnerRole(String googleProject);
  /**
   * Grants permissions to run Google lifescience jobs for the provideded user email.
   *
   * <p>The users's Terra PET service account will get: lifescienceRunner and serviceAccountUser(on
   * the petSA itself).
   */
  void grantWorkflowRunnerRoleAsService(String googleProject, String userEmail);
}
