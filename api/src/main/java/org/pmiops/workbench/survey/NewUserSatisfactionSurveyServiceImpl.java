package org.pmiops.workbench.survey;

import jakarta.mail.MessagingException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import javax.inject.Provider;
import javax.transaction.Transactional;
import org.apache.arrow.util.VisibleForTesting;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.NewUserSatisfactionSurveyDao;
import org.pmiops.workbench.db.dao.NewUserSatisfactionSurveyOneTimeCodeDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurveyOneTimeCode;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.CreateNewUserSatisfactionSurvey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NewUserSatisfactionSurveyServiceImpl implements NewUserSatisfactionSurveyService {
  private final Clock clock;
  private final NewUserSatisfactionSurveyOneTimeCodeDao newUserSatisfactionSurveyOneTimeCodeDao;
  private final NewUserSatisfactionSurveyMapper newUserSatisfactionSurveyMapper;
  private final NewUserSatisfactionSurveyDao newUserSatisfactionSurveyDao;
  private final UserDao userDao;
  private final MailService mailService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @VisibleForTesting static final int TWO_WEEKS_DAYS = 2 * 7;
  @VisibleForTesting static final int TWO_MONTHS_DAYS = 61;

  @Autowired
  public NewUserSatisfactionSurveyServiceImpl(
      Clock clock,
      NewUserSatisfactionSurveyOneTimeCodeDao newUserSatisfactionSurveyOneTimeCodeDao,
      NewUserSatisfactionSurveyMapper newUserSatisfactionSurveyMapper,
      NewUserSatisfactionSurveyDao newUserSatisfactionSurveyDao,
      UserDao userDao,
      MailService mailService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.clock = clock;
    this.newUserSatisfactionSurveyOneTimeCodeDao = newUserSatisfactionSurveyOneTimeCodeDao;
    this.newUserSatisfactionSurveyMapper = newUserSatisfactionSurveyMapper;
    this.newUserSatisfactionSurveyDao = newUserSatisfactionSurveyDao;
    this.userDao = userDao;
    this.mailService = mailService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public boolean eligibleToTakeSurvey(DbUser user) {
    // Logic in this method is duplicated in emailNewUserSatisfactionSurveyLinks to improve cron
    // performance. If you update one, you should update the other.
    if (user.getNewUserSatisfactionSurvey() != null) {
      return false;
    }

    final Instant now = clock.instant();
    return now.isAfter(eligibilityWindowStart(user)) && now.isBefore(eligibilityWindowEnd(user));
  }

  private Instant eligibilityWindowStart(DbUser user) {
    final Instant createdTime = user.getCreationTime().toInstant();

    return createdTime.plus(TWO_WEEKS_DAYS, ChronoUnit.DAYS);
  }

  @Override
  public Instant eligibilityWindowEnd(DbUser user) {
    return eligibilityWindowStart(user).plus(TWO_MONTHS_DAYS, ChronoUnit.DAYS);
  }

  private boolean oneTimeCodeValid(DbNewUserSatisfactionSurveyOneTimeCode oneTimeCode) {
    return oneTimeCode.getUsedTime() == null && eligibleToTakeSurvey(oneTimeCode.getUser());
  }

  @Override
  public boolean oneTimeCodeStringValid(String oneTimeCode) {
    try {
      return newUserSatisfactionSurveyOneTimeCodeDao
          .findById(UUID.fromString(oneTimeCode))
          .map(this::oneTimeCodeValid)
          .orElse(false);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Override
  @Transactional
  public void createNewUserSatisfactionSurveyWithOneTimeCode(
      CreateNewUserSatisfactionSurvey createNewUserSatisfactionSurvey, String oneTimeCode) {
    DbNewUserSatisfactionSurveyOneTimeCode dbNewUserSatisfactionSurveyOneTimeCode;
    try {
      dbNewUserSatisfactionSurveyOneTimeCode =
          newUserSatisfactionSurveyOneTimeCodeDao
              .findById(UUID.fromString(oneTimeCode))
              .orElseThrow(ForbiddenException::new);
    } catch (IllegalArgumentException e) {
      throw new ForbiddenException();
    }
    if (!oneTimeCodeValid(dbNewUserSatisfactionSurveyOneTimeCode)) {
      throw new ForbiddenException();
    }
    newUserSatisfactionSurveyDao.save(
        newUserSatisfactionSurveyMapper.toDbNewUserSatisfactionSurvey(
            createNewUserSatisfactionSurvey, dbNewUserSatisfactionSurveyOneTimeCode.getUser()));
    dbNewUserSatisfactionSurveyOneTimeCode =
        newUserSatisfactionSurveyOneTimeCodeDao.save(
            dbNewUserSatisfactionSurveyOneTimeCode.setUsedTime(Timestamp.from(clock.instant())));
  }

  @Override
  public void emailNewUserSatisfactionSurveyLinks() {
    // Logic in this call is duplicated from eligibleToTakeSurvey to improve cron performance.
    // If you update one, you should update the other.
    final List<DbUser> eligibleUsers =
        userDao.findUsersBetweenCreationTimeWithoutNewUserSurveyOrCode(
            Timestamp.from(
                clock.instant().minus(TWO_WEEKS_DAYS + TWO_MONTHS_DAYS, ChronoUnit.DAYS)),
            Timestamp.from(clock.instant().minus(TWO_WEEKS_DAYS, ChronoUnit.DAYS)));
    for (DbUser user : eligibleUsers) {
      DbNewUserSatisfactionSurveyOneTimeCode dbNewUserSatisfactionSurveyOneTimeCode =
          newUserSatisfactionSurveyOneTimeCodeDao.save(
              new DbNewUserSatisfactionSurveyOneTimeCode().setUser(user));
      final String surveyLink =
          String.format(
              "%s?surveyCode=%s",
              workbenchConfigProvider.get().server.uiBaseUrl,
              dbNewUserSatisfactionSurveyOneTimeCode.getId().toString());
      try {
        mailService.sendNewUserSatisfactionSurveyEmail(user, surveyLink);
      } catch (MessagingException e) {
        throw new ServerErrorException(
            String.format(
                "Failed to send new user satisfaction survey email to user %s with code %s",
                user.getUserId(), dbNewUserSatisfactionSurveyOneTimeCode.getId()),
            e);
      }
    }
  }
}
