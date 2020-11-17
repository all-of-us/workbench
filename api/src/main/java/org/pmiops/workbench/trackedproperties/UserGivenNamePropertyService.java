package org.pmiops.workbench.trackedproperties;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.pmiops.workbench.actionaudit.auditors.ProfileAuditor;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.profile.ProfileMapper;
import org.springframework.stereotype.Service;

@Service
public class UserGivenNamePropertyService implements TrackedProperty<DbUser, String> {

  private static final int MAX_NAME_LENGTH = 256;
  private final ProfileAuditor profileAuditor;

  @Override
  public PropertyModifiability getModifiability() {
    return PropertyModifiability.USER_WRITEABLE;
  }

  private final ProfileMapper profileMapper;
  private final UserDao userDao;

  public UserGivenNamePropertyService(
      ProfileAuditor profileAuditor,
      ProfileMapper profileMapper,
      UserDao userDao) {
    this.profileAuditor = profileAuditor;
    this.profileMapper = profileMapper;
    this.userDao = userDao;
  }

  @Override
  public boolean isValid(DbUser target, String newValue) {
    return !Strings.isNullOrEmpty(newValue) && newValue.length() < MAX_NAME_LENGTH;
  }

  @Override
  public Function<DbUser, String> getAccessor() {
    return DbUser::getGivenName;
  }

  @Override
  public Optional<BiFunction<DbUser, String, DbUser>> getMutator() {
    return Optional.of((t, p) -> {
      t.setGivenName(p);
      return t;
    });
  }

  @Override
  public void auditChange(DbUser target, Optional<String> previousValue,
      Optional<String> newValue) {
    // use mapper, etc, as needed
     final Profile updatedProfile  = profileMapper.toModel(
         target,
         null,
         null,
         null,
         null);
     profileAuditor.fireUpdateAction(null, updatedProfile); // fixme
  }

  @Override
  public void notifyUser(String emailAddress, String text) {

  }

  @Override
  public Function<DbUser, DbUser> getCommitter() {
    return userDao::save;
  }
}
