package org.pmiops.workbench.db.dao;

import java.util.List;

public interface InitialCreditsExpirationService {
  void checkCreditsExpirationForUserIDs(List<Long> userIdsList);
}
