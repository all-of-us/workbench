package org.pmiops.workbench.trackables;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.pmiops.workbench.actionaudit.auditors.ProfileAuditor;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.stereotype.Service;

@Service
public class UserGivenNamePropertyService implements TrackableProperty<DbUser, String> {

  private static final int MAX_NAME_LENGTH = 256;
  private final ProfileAuditor profileAuditor;
  private final UserDao userDao;

  public UserGivenNamePropertyService(ProfileAuditor profileAuditor,
      UserDao userDao) {
    this.profileAuditor = profileAuditor;
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
  public BiFunction<DbUser, String, DbUser> getMutator() {
    return (t, p) -> {
      t.setGivenName(p);
      return t;
    };
  }

  @Override
  public void auditChange(DbUser target, Optional<String> previousValue,
      Optional<String> newValue) {
    // use mapper, etc, as needed
    profileAuditor.fireUpdateAction(null, null); // fixme
  }

  @Override
  public void notifyUser(String emailAddress, String text) {

  }

  @Override
  public Function<DbUser, DbUser> getCommitter() {
    return userDao::save;
  }
}
