package org.pmiops.workbench.impersonation;

import java.io.IOException;
import javax.annotation.Nonnull;
import org.pmiops.workbench.db.model.DbUser;

public interface ImpersonatedFirecloudService {
  // accept the Terra Terms of Service on behalf of a user by using impersonation
  void acceptTermsOfService(@Nonnull DbUser dbUser) throws IOException;

  boolean getUserTermsOfServiceStatus(@Nonnull DbUser dbUser) throws IOException;
}
