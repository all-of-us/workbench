package org.pmiops.workbench.iam;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.firecloud.ApiException;

public interface IamService {

  Optional<String> getOrCreatePetServiceAccountUsingImpersonation(
      String googleProject, String userEmail) throws IOException, ApiException;
}
