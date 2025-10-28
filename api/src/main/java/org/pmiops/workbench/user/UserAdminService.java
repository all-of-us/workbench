package org.pmiops.workbench.user;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.UserDisabledEventDao;
import org.pmiops.workbench.db.dao.UserEgressBypassWindowDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserEgressBypassWindow;
import org.pmiops.workbench.model.EgressBypassWindow;
import org.pmiops.workbench.model.EgressVwbBypassWindow;
import org.pmiops.workbench.model.UserDisabledEvent;
import org.pmiops.workbench.utils.mappers.EgressBypassWindowMapper;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.vwb.exfil.ExfilManagerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserAdminService {
  private static final Integer BYPASS_PERIOD_IN_DAY = 2;
  private final UserEgressBypassWindowDao userEgressBypassWindowDao;
  private final EgressBypassWindowMapper egressBypassWindowMapper;
  private final UserDisabledEventDao userDisabledEventDao;
  private final UserMapper userMapper;
  private final UserService userService;
  private final ExfilManagerClient exfilManagerClient;
  private final Clock clock;

  @Autowired
  public UserAdminService(
      UserEgressBypassWindowDao userEgressBypassWindowDao,
      EgressBypassWindowMapper egressBypassWindowMapper,
      UserDisabledEventDao userDisabledEventDao,
      UserMapper userMapper,
      UserService userService,
      ExfilManagerClient exfilManagerClient,
      Clock clock) {
    this.userEgressBypassWindowDao = userEgressBypassWindowDao;
    this.egressBypassWindowMapper = egressBypassWindowMapper;
    this.userDisabledEventDao = userDisabledEventDao;
    this.userMapper = userMapper;
    this.userService = userService;
    this.exfilManagerClient = exfilManagerClient;
    this.clock = clock;
  }

  /**
   * Creates an egress bypass window for a user.
   *
   * @param userId The user ID
   * @param startTime The start time of the bypass window
   * @param description A description of the bypass request
   * @param vwbWorkspaceUfid Optional VWB workspace UFID. If provided, also creates an egress
   *     threshold override in the exfil manager.
   */
  public void createEgressBypassWindow(
      Long userId, Instant startTime, String description, String vwbWorkspaceUfid) {
    Instant endTime = startTime.plus(BYPASS_PERIOD_IN_DAY, ChronoUnit.DAYS);

    // Save to database
    userEgressBypassWindowDao.save(
        new DbUserEgressBypassWindow()
            .setUserId(userId)
            .setStartTime(Timestamp.from(startTime))
            .setEndTime(Timestamp.from(endTime))
            .setDescription(description)
            .setVwbWorkspaceUfid(vwbWorkspaceUfid));

    // If VWB workspace UFID is provided, call exfil manager to create egress threshold override
    if (vwbWorkspaceUfid != null) {
      DbUser user = userService.getByDatabaseId(userId).orElseThrow();
      exfilManagerClient.createEgressThresholdOverride(
          user.getUsername(), vwbWorkspaceUfid, endTime, description);
    }
  }

  /**
   * Creates a regular egress bypass window (without VWB workspace).
   *
   * @param userId The user ID
   * @param startTime The start time of the bypass window
   * @param description A description of the bypass request
   */
  public void createEgressBypassWindow(Long userId, Instant startTime, String description) {
    createEgressBypassWindow(userId, startTime, description, null);
  }

  public EgressBypassWindow getCurrentEgressBypassWindow(Long userId) {
    Timestamp now = Timestamp.from(clock.instant());
    return userEgressBypassWindowDao.getByUserIdOrderByStartTimeDesc(userId).stream()
        .filter(t -> t.getStartTime().before(now) && t.getEndTime().after(now))
        .findFirst()
        .map(egressBypassWindowMapper::toApiEgressBypassWindow)
        .orElse(null);
  }

  public List<EgressBypassWindow> listAllEgressBypassWindows(Long userId) {
    return userEgressBypassWindowDao.getByUserIdOrderByStartTimeDesc(userId).stream()
        .map(egressBypassWindowMapper::toApiEgressBypassWindow)
        .collect(Collectors.toList());
  }

  public List<UserDisabledEvent> listAllUserDisabledEvents(Long userId) {
    return userDisabledEventDao.getByUserIdOrderByUpdateTimeDesc(userId).stream()
        .map(userMapper::toApiUserDisabledEvent)
        .toList();
  }

  public List<EgressVwbBypassWindow> listAllVwbEgressBypassWindows(Long userId) {
    return userEgressBypassWindowDao.getByUserIdOrderByStartTimeDesc(userId).stream()
        .filter(window -> window.getVwbWorkspaceUfid() != null)
        .map(egressBypassWindowMapper::toApiEgressVwbBypassWindow)
        .collect(Collectors.toList());
  }
}
