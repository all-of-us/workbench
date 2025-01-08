package org.pmiops.workbench.exfiltration.impl;

import static org.pmiops.workbench.exfiltration.ExfiltrationUtils.EGRESS_VWB_JIRA_HANDLER_QUALIFIER;
import static org.pmiops.workbench.leonardo.LeonardoAppUtils.appServiceNameToAppType;

import com.google.common.base.Strings;
import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import java.time.Clock;
import java.util.logging.Logger;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exfiltration.EgressRemediationAction;
import org.pmiops.workbench.exfiltration.EgressRemediationService;
import org.pmiops.workbench.exfiltration.ExfiltrationUtils;
import org.pmiops.workbench.exfiltration.jirahandler.EgressJiraHandler;
import org.pmiops.workbench.jira.ApiException;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoAppUtils;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.SumologicEgressEvent;
import org.pmiops.workbench.user.UserAdminService;
import org.pmiops.workbench.utils.mappers.EgressEventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service(ExfiltrationUtils.EGRESS_VWB_SERVICE_QUALIFIER)
public class EgressVwbRemediationService extends EgressRemediationService {
  private static final Logger logger =
      Logger.getLogger(EgressVwbRemediationService.class.getName());

  // The maximum egress user is allowed when bypassed for large file download. If more than this
  // user will still trigger egress alert.
  // Current value is 100GB
  private static final int EGRESS_HARD_LIMIT_MB = 100 * 1024;

  private final EgressJiraHandler egressJiraHandler;
  private final MailService mailService;
  private final EgressEventMapper egressEventMapper;
  private final UserAdminService userAdminService;

  @Autowired
  public EgressVwbRemediationService(
      Clock clock,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserService userService,
      LeonardoApiClient leonardoNotebooksClient,
      EgressEventAuditor egressEventAuditor,
      EgressEventDao egressEventDao,
      EgressEventMapper egressEventMapper,
      @Qualifier(EGRESS_VWB_JIRA_HANDLER_QUALIFIER) EgressJiraHandler egressJiraHandler,
      MailService mailService,
      UserAdminService userAdminService) {
    super(
        clock,
        workbenchConfigProvider,
        userService,
        leonardoNotebooksClient,
        egressEventAuditor,
        egressEventDao);
    this.egressJiraHandler = egressJiraHandler;
    this.mailService = mailService;
    this.egressEventMapper = egressEventMapper;
    this.userAdminService = userAdminService;
  }

  @Override
  protected void sendEgressRemediationEmail(
      DbUser user, EgressRemediationAction action, DbEgressEvent event) throws MessagingException {
    SumologicEgressEvent originalEvent = egressEventMapper.toSumoLogicEvent(event);
    String environmentType =
        appServiceNameToAppType(Strings.nullToEmpty(originalEvent.getSrcGkeServiceName()))
            .map(LeonardoAppUtils::appDisplayName)
            .orElse("Jupyter");
    mailService.sendEgressRemediationEmailForVwb(user, action);
  }

  @Override
  protected void logEvent(DbEgressEvent event, EgressRemediationAction action) throws ApiException {
    egressJiraHandler.logEventToJira(event, action);
  }

  @Override
  protected boolean shouldSkipEgressEvent(DbEgressEvent event) {
    if (isUserBypassedForLargeFileDownload(event)) {
      logger.info(
          String.format(
              "Skip egress event %d because user is bypassed for large file download",
              event.getEgressEventId()));
      return true;
    }
    return false;
  }

  @Override
  protected int getEgressIncidentCountForUser(DbEgressEvent event, DbUser user) {
    return event.getVwbIncidentCount();
  }

  private boolean isUserBypassedForLargeFileDownload(DbEgressEvent event) {
    return userAdminService.getCurrentEgressBypassWindow(event.getUser().getUserId()) != null
        && event.getEgressMegabytes() != null
        && event.getEgressMegabytes() < EGRESS_HARD_LIMIT_MB;
  }
}
