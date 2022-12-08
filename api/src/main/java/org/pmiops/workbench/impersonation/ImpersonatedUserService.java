package org.pmiops.workbench.impersonation;

/**
 * An impersonation-enabled version of {@link org.pmiops.workbench.db.dao.UserService}
 *
 * <p>REMINDER: With great power comes great responsibility. Impersonation should not be used in
 * production, except where absolutely necessary.
 */
public interface ImpersonatedUserService {
  // retrieve a user's Terra Terms of Service status, by using impersonation
  boolean getUserTerraTermsOfServiceStatus(String username);

  // accepts the latest Terra Terms of Service on behalf of a user by using impersonation
  void acceptTerraTermsOfService(String username);
}
