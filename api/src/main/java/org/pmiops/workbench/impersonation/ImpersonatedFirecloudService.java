package org.pmiops.workbench.impersonation;

import java.io.IOException;
import javax.annotation.Nonnull;
import org.pmiops.workbench.db.model.DbUser;

/**
 * An impersonation-enabled version of {@link org.pmiops.workbench.firecloud.FireCloudService}
 *
 * <p>REMINDER: With great power comes great responsibility. Impersonation should not be used in
 * production, except where absolutely necessary.
 */
public interface ImpersonatedFirecloudService {
  // accept the Terra Terms of Service on behalf of a user by using impersonation
  void acceptTermsOfService(@Nonnull DbUser dbUser) throws IOException;

  // retrieve a user's Terra Terms of Service status, by using impersonation
  boolean getUserTermsOfServiceStatus(@Nonnull DbUser dbUser) throws IOException;
}
