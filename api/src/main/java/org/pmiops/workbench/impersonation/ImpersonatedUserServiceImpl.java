package org.pmiops.workbench.impersonation;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import org.broadinstitute.dsde.workbench.client.sam.api.TermsOfServiceApi;
import org.broadinstitute.dsde.workbench.client.sam.model.UserTermsOfServiceDetails;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserTermsOfServiceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.sam.SamApiClientFactory;
import org.pmiops.workbench.sam.SamRetryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * An impersonation-enabled version of a subset of {@link
 * org.pmiops.workbench.db.dao.UserServiceImpl}
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
  private final SamApiClientFactory samApiClientFactory;
  private final SamRetryHandler samRetryHandler;

  @Autowired
  public ImpersonatedUserServiceImpl(
      Clock clock,
      ImpersonatedFirecloudService impersonatedFirecloudService,
      UserDao userDao,
      UserTermsOfServiceDao userTermsOfServiceDao,
      SamApiClientFactory samApiClientFactory,
      SamRetryHandler samRetryHandler) {
    this.clock = clock;
    this.impersonatedFirecloudService = impersonatedFirecloudService;
    this.userDao = userDao;
    this.userTermsOfServiceDao = userTermsOfServiceDao;
    this.samApiClientFactory = samApiClientFactory;
    this.samRetryHandler = samRetryHandler;
  }

  @Override
  public UserTermsOfServiceDetails getTerraTermsOfServiceStatusForUser(String username) {
    try {
      var samClient = samApiClientFactory.newImpersonatedApiClient(username);
      return samRetryHandler.run(
          context -> new TermsOfServiceApi(samClient).userTermsOfServiceGetSelf());
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
  }

  @Override
  public void acceptTerraTermsOfServiceForUser(String username) {
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

    // TODO: save the terra tos version as well, when that's available
    // after RW-11433 or RW-11434
  }
}
