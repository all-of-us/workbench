package org.pmiops.workbench.identityverification;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.db.dao.IdentityVerificationDao;
import org.pmiops.workbench.db.model.DbIdentityVerification;
import org.pmiops.workbench.db.model.DbIdentityVerification.DbIdentityVerificationSystem;
import org.pmiops.workbench.db.model.DbUser;

@ExtendWith(MockitoExtension.class)
public class IdentityVerificationServiceTest {
  @Mock IdentityVerificationDao mockIdentityVerificationDao;
  @InjectMocks IdentityVerificationService identityVerificationService;

  @Test
  public void updateIdentityVerificationSystem_update() {
    DbUser user = new DbUser();
    user.setUserId(5L);
    DbIdentityVerification identityVerification = new DbIdentityVerification();
    identityVerification.setIdentityVerificationId(7L);
    identityVerification.setUser(user);
    when(mockIdentityVerificationDao.getByUser(user)).thenReturn(Optional.of(identityVerification));

    identityVerificationService.updateIdentityVerificationSystem(
        user, DbIdentityVerificationSystem.ID_ME);

    verify(mockIdentityVerificationDao).getByUser(user);
    ArgumentCaptor<DbIdentityVerification> argument =
        ArgumentCaptor.forClass(DbIdentityVerification.class);
    verify(mockIdentityVerificationDao).save(argument.capture());
    Assertions.assertEquals(
        DbIdentityVerificationSystem.ID_ME, argument.getValue().getIdentityVerificationSystem());
    Assertions.assertEquals(user, argument.getValue().getUser());
  }

  @Test
  public void updateIdentityVerificationSystem_create() {
    DbUser user = new DbUser();
    user.setUserId(5L);
    when(mockIdentityVerificationDao.getByUser(user)).thenReturn(Optional.empty());

    identityVerificationService.updateIdentityVerificationSystem(
        user, DbIdentityVerificationSystem.ID_ME);

    verify(mockIdentityVerificationDao).getByUser(user);
    ArgumentCaptor<DbIdentityVerification> argument =
        ArgumentCaptor.forClass(DbIdentityVerification.class);
    verify(mockIdentityVerificationDao).save(argument.capture());
    Assertions.assertEquals(
        DbIdentityVerificationSystem.ID_ME, argument.getValue().getIdentityVerificationSystem());
    Assertions.assertEquals(user, argument.getValue().getUser());
  }
}
