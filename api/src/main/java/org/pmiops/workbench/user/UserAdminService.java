package org.pmiops.workbench.user;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.pmiops.workbench.db.dao.UserEgressBypassWindowDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserEgressBypassWindow;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.CreateEgressBypassWindowRequest;
import org.pmiops.workbench.model.EgressBypassWindow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class UserAdminService {
  private final UserService userService;
  private final UserEgressBypassWindowDao userEgressBypassWindowDao;
  private final Clock clock;


  @Autowired
  public UserAdminService(UserService userService,
      UserEgressBypassWindowDao userEgressBypassWindowDao, Clock clock) {
    this.userService = userService;
    this.userEgressBypassWindowDao = userEgressBypassWindowDao;
    this.clock = clock;
  }

  public ResponseEntity<EgressBypassWindow> createEgressBypassWindow(Long userId,
      CreateEgressBypassWindowRequest request) {
    Set<DbUserEgressBypassWindow> dbUserEgressBypassWindows = userEgressBypassWindowDao.getByUserUserIdOrderByStartTimeDesc(userId);
    if(!hasActiveEgressBypassWindow(dbUserEgressBypassWindows, clock.instant())) {
      Timestamp startTime = new Timestamp(request.getStartTime());
      Timestamp endTime = Timestamp.from(startTime.toInstant().plus(2, ChronoUnit.DAYS));
      userEgressBypassWindowDao.save(new DbUserEgressBypassWindow().setUserId(userId).setStartTime(startTime).setEndTime(endTime).setDescription();
    } else {
      throw new ForbiddenException("User already has an active bypass session.");
    }

    return new ResponseEntity<EgressBypassWindow>(HttpStatus.OK);
  }

  public ResponseEntity<Void> deleteEgressBypassWindow() {
    // do some magic!
    return new ResponseEntity<Void>(HttpStatus.OK);
  }

  public ResponseEntity<EgressBypassWindow> getEgressBypassWindow() {
    // do some magic!
    return new ResponseEntity<EgressBypassWindow>(HttpStatus.OK);
  }

  /**
   * Returns {@code true} if current timestamp is between any {@code DbUserEgressBypassWindow}
   * start time and end time. There should be no overlap because we don't allow creating new one
   * if user have active one.
   *
   */
  private Boolean hasActiveEgressBypassWindow(Set<DbUserEgressBypassWindow> dbUserEgressBypassWindows, Instant now) {
    Timestamp timestampNow = Timestamp.from(now);
    return dbUserEgressBypassWindows.stream().anyMatch(t -> t.getStartTime().after(timestampNow) && t.getEndTime().before(timestampNow));
  }
}
