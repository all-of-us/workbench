package org.pmiops.workbench.impersonation;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserTermsOfServiceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * An impersonation-enabled version of {@link org.pmiops.workbench.db.dao.UserServiceImpl}
 *
 * <p>REMINDER: With great power comes great responsibility. Impersonation should not be used in
 * production, except where absolutely necessary.
 */
@Service
public class ImpersonatedUserServiceImpl implements ImpersonatedUserService {

  private final Clock clock;
  private final ImpersonatedFirecloudService impersonatedFirecloudService;
  private final UserDao userDao;
  private final UserTermsOfServiceDao userTermsOfServiceDao;

  @Autowired
  public ImpersonatedUserServiceImpl(
      Clock clock,
      ImpersonatedFirecloudService impersonatedFirecloudService,
      UserDao userDao,
      UserTermsOfServiceDao userTermsOfServiceDao) {
    this.clock = clock;
    this.impersonatedFirecloudService = impersonatedFirecloudService;
    this.userDao = userDao;
    this.userTermsOfServiceDao = userTermsOfServiceDao;
  }

  @Override
  public boolean getUserTerraTermsOfServiceStatus(String username) {
    final DbUser dbUser = userDao.findUserByUsername(username);

    try {
      return impersonatedFirecloudService.getUserTermsOfServiceStatus(dbUser);
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
  }

  @Override
  public void acceptTerraTermsOfService(String username) {
    final DbUser dbUser = userDao.findUserByUsername(username);

    try {
      impersonatedFirecloudService.acceptTermsOfService(dbUser);
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }

    userTermsOfServiceDao.save(
        userTermsOfServiceDao
            .findByUserIdOrThrow(dbUser.getUserId())
            .setTerraAgreementTime(new Timestamp(clock.instant().toEpochMilli())));
  }
}
