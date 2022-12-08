package org.pmiops.workbench.impersonation;

public interface ImpersonatedUserService {

  boolean getUserTerraTermsOfServiceStatus(String username);

  // accepts the latest Terra Terms of Service on behalf of a user by using impersonation
  void acceptTerraTermsOfService(String username);
}
