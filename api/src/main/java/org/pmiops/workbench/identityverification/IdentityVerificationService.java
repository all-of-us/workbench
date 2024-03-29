package org.pmiops.workbench.identityverification;

import org.pmiops.workbench.db.dao.IdentityVerificationDao;
import org.pmiops.workbench.db.model.DbIdentityVerification;
import org.pmiops.workbench.db.model.DbIdentityVerification.DbIdentityVerificationSystem;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IdentityVerificationService {

  private final IdentityVerificationDao identityVerificationDao;

  @Autowired
  public IdentityVerificationService(IdentityVerificationDao identityVerificationDao) {
    this.identityVerificationDao = identityVerificationDao;
  }

  public void updateIdentityVerificationSystem(
      DbUser user, DbIdentityVerificationSystem identityVerificationSystem) {
    identityVerificationDao.save(
        retrieveIdentityVerificationOrCreate(user)
            .setIdentityVerificationSystem(identityVerificationSystem));
  }

  private DbIdentityVerification retrieveIdentityVerificationOrCreate(DbUser user) {
    return identityVerificationDao
        .getByUser(user)
        .orElse(new DbIdentityVerification().setUser(user));
  }
}
