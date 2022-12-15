package org.pmiops.workbench.survey;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.inject.Provider;
import javax.mail.MessagingException;
import javax.transaction.Transactional;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.NewUserSatisfactionSurveyDao;
import org.pmiops.workbench.db.dao.OneTimeCodeDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbOneTimeCode;
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
  private final OneTimeCodeDao oneTimeCodeDao;
  private final NewUserSatisfactionSurveyMapper newUserSatisfactionSurveyMapper;
  private final NewUserSatisfactionSurveyDao newUserSatisfactionSurveyDao;
  private final UserDao userDao;
  private final MailService mailService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public NewUserSatisfactionSurveyServiceImpl(
      Clock clock,
      OneTimeCodeDao oneTimeCodeDao,
      NewUserSatisfactionSurveyMapper newUserSatisfactionSurveyMapper,
      NewUserSatisfactionSurveyDao newUserSatisfactionSurveyDao,
      UserDao userDao,
      MailService mailService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.clock = clock;
    this.oneTimeCodeDao = oneTimeCodeDao;
    this.newUserSatisfactionSurveyMapper = newUserSatisfactionSurveyMapper;
    this.newUserSatisfactionSurveyDao = newUserSatisfactionSurveyDao;
    this.userDao = userDao;
    this.mailService = mailService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public boolean eligibleToTakeSurvey(DbUser user) {
    if (user.getNewUserSatisfactionSurvey() != null) {
      return false;
    }

    final Instant now = clock.instant();
    return now.isAfter(eligibilityWindowStart(user)) && now.isBefore(eligibilityWindowEnd(user));
  }

  private Instant eligibilityWindowStart(DbUser user) {
    final Instant createdTime = user.getCreationTime().toInstant();

    return createdTime.plus(2 * 7, ChronoUnit.DAYS);
  }

  @Override
  public Instant eligibilityWindowEnd(DbUser user) {
    return eligibilityWindowStart(user).plus(61, ChronoUnit.DAYS);
  }

  private boolean oneTimeCodeValid(DbOneTimeCode oneTimeCode) {
    return oneTimeCode.getUsedTime() == null && eligibleToTakeSurvey(oneTimeCode.getUser());
  }

  @Override
  public boolean oneTimeCodeStringValid(String oneTimeCode) {
    return oneTimeCodeDao.findByStringId(oneTimeCode).map(this::oneTimeCodeValid).orElse(false);
  }

  @Override
  @Transactional
  public void createNewUserSatisfactionSurveyWithOneTimeCode(
      CreateNewUserSatisfactionSurvey createNewUserSatisfactionSurvey, String oneTimeCode) {
    DbOneTimeCode dbOneTimeCode =
        oneTimeCodeDao.findByStringId(oneTimeCode).orElseThrow(ForbiddenException::new);
    if (!oneTimeCodeValid(dbOneTimeCode)) {
      throw new ForbiddenException();
    }
    newUserSatisfactionSurveyDao.save(
        newUserSatisfactionSurveyMapper.toDbNewUserSatisfactionSurvey(
            createNewUserSatisfactionSurvey, dbOneTimeCode.getUser()));
    dbOneTimeCode = oneTimeCodeDao.save(dbOneTimeCode.setUsedTime(Timestamp.from(clock.instant())));
  }

  @Override
  public void emailNewUserSatisfactionSurveyLinks() {
    for (DbUser user : userDao.findAll()) {
      final boolean haveNotEmailedSurvey = user.getOneTimeCode() == null;
      if (haveNotEmailedSurvey && eligibleToTakeSurvey(user)) {
        DbOneTimeCode dbOneTimeCode = oneTimeCodeDao.save(new DbOneTimeCode().setUser(user));
        final String surveyLink =
            String.format(
                "%s?surveyCode=%s",
                workbenchConfigProvider.get().server.uiBaseUrl, dbOneTimeCode.getId().toString());
        try {
          mailService.sendNewUserSatisfactionSurveyEmail(user, surveyLink);
        } catch (MessagingException e) {
          throw new ServerErrorException("Failed to send new user satisfaction survey email", e);
        }
      }
    }
  }
}
