package org.pmiops.workbench.user;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import org.pmiops.workbench.db.dao.UserEgressBypassWindowDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUserEgressBypassWindow;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.model.EgressBypassWindow;
import org.pmiops.workbench.utils.mappers.EgressBypassWindowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class UserAdminService {
  private final UserService userService;
  private final UserEgressBypassWindowDao userEgressBypassWindowDao;
  private final EgressBypassWindowMapper egressBypassWindowMapper;
  private final Clock clock;


  @Autowired
  public UserAdminService(UserService userService,
      UserEgressBypassWindowDao userEgressBypassWindowDao,
      EgressBypassWindowMapper egressBypassWindowMapper, Clock clock) {
    this.userService = userService;
    this.userEgressBypassWindowDao = userEgressBypassWindowDao;
    this.egressBypassWindowMapper = egressBypassWindowMapper;
    this.clock = clock;
  }

  public void createEgressBypassWindow(Long userId,
     Instant startTime, String description) {
    Set<DbUserEgressBypassWindow> dbUserEgressBypassWindows = userEgressBypassWindowDao.getByUserUserIdOrderByStartTimeDesc(userId);
    if(getActiveEgressBypassWindow(dbUserEgressBypassWindows, clock.instant()).isEmpty()) {
      Instant endTime = startTime.plus(2, ChronoUnit.DAYS);
      userEgressBypassWindowDao.save(new DbUserEgressBypassWindow().setUserId(userId).setStartTime(Timestamp.from(startTime)).setEndTime(Timestamp.from(endTime)).setDescription(description));
    } else {
      throw new ForbiddenException("User already has an active bypass session.");
    }
    
  }

  public ResponseEntity<EgressBypassWindow> getCurrentEgressBypassWindow(Long userId) {
    Set<DbUserEgressBypassWindow> dbUserEgressBypassWindows = userEgressBypassWindowDao.getByUserUserIdOrderByStartTimeDesc(userId);
    getActiveEgressBypassWindow(dbUserEgressBypassWindows, clock.instant());
    return ResponseEntity.ok(egressBypassWindowMapper.toApiEgressBypassWindow(getActiveEgressBypassWindow(dbUserEgressBypassWindows, clock.instant()).orElse(null)));
  }

  /**
   * Returns {@code true} if current timestamp is between any {@code DbUserEgressBypassWindow}
   * start time and end time. There should be no overlap because we don't allow creating new one
   * if user have active one.
   *
   */
  private Optional<DbUserEgressBypassWindow> getActiveEgressBypassWindow(Set<DbUserEgressBypassWindow> dbUserEgressBypassWindows, Instant now) {
    Timestamp timestampNow = Timestamp.from(now);
    return dbUserEgressBypassWindows.stream().filter(t -> t.getStartTime().after(timestampNow) && t.getEndTime().before(timestampNow)).findFirst();
  }
}
