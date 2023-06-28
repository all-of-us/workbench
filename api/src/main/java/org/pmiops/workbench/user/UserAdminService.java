package org.pmiops.workbench.user;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.pmiops.workbench.db.dao.UserEgressBypassWindowDao;
import org.pmiops.workbench.db.model.DbUserEgressBypassWindow;
import org.pmiops.workbench.model.EgressBypassWindow;
import org.pmiops.workbench.utils.mappers.EgressBypassWindowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserAdminService {
  private static final Integer BYPASS_PERIOD_IN_DAY = 2;
  private final UserEgressBypassWindowDao userEgressBypassWindowDao;
  private final EgressBypassWindowMapper egressBypassWindowMapper;
  private final Clock clock;

  @Autowired
  public UserAdminService(
      UserEgressBypassWindowDao userEgressBypassWindowDao,
      EgressBypassWindowMapper egressBypassWindowMapper,
      Clock clock) {
    this.userEgressBypassWindowDao = userEgressBypassWindowDao;
    this.egressBypassWindowMapper = egressBypassWindowMapper;
    this.clock = clock;
  }

  public void createEgressBypassWindow(Long userId, Instant startTime, String description) {
    Instant endTime = startTime.plus(BYPASS_PERIOD_IN_DAY, ChronoUnit.DAYS);
    userEgressBypassWindowDao.save(
        new DbUserEgressBypassWindow()
            .setUserId(userId)
            .setStartTime(Timestamp.from(startTime))
            .setEndTime(Timestamp.from(endTime))
            .setDescription(description));
  }

  public EgressBypassWindow getCurrentEgressBypassWindow(Long userId) {
    Timestamp now = Timestamp.from(clock.instant());
    return userEgressBypassWindowDao.getByUserIdOrderByStartTimeDesc(userId).stream()
        .filter(t -> t.getStartTime().before(now) && t.getEndTime().after(now))
        .findFirst()
        .map(egressBypassWindowMapper::toApiEgressBypassWindow)
        .orElse(null);
  }
}
