package org.pmiops.workbench.access;

import org.pmiops.workbench.db.model.DbUser;
import org.springframework.stereotype.Service;

@Service
public class InvalidKeyAccessModule implements AccessModuleService {

  public InvalidKeyAccessModule() {
  }

  @Override
  public AccessScore scoreUser(DbUser user) {
    return AccessScore.INVALID_ACCESS_MODULE;
  }
}
