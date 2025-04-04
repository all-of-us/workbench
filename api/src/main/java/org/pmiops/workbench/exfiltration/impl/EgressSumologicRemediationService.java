package org.pmiops.workbench.exfiltration.impl;

import static org.pmiops.workbench.exfiltration.ExfiltrationUtils.SUMOLOGIC_JIRA_HANDLER_QUALIFIER;
import static org.pmiops.workbench.leonardo.LeonardoAppUtils.appServiceNameToAppType;

import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import java.time.Clock;
import java.util.Optional;
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
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.user.UserAdminService;
import org.pmiops.workbench.utils.mappers.SumologicEgressEventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service(ExfiltrationUtils.EGRESS_SUMOLOGIC_SERVICE_QUALIFIER)
public class EgressSumologicRemediationService extends EgressRemediationService {
  private static final Logger logger =
      Logger.getLogger(EgressSumologicRemediationService.class.getName());

  // The maximum egress user is allowed when bypassed for large file download. If more than this
  // user will still trigger egress alert.
  // Current value is 100GB
  private static final int EGRESS_HARD_LIMIT_MB = 100 * 1024;

  private final EgressJiraHandler egressJiraHandler;
  private final MailService mailService;
  private final SumologicEgressEventMapper sumologicEgressEventMapper;
  private final UserAdminService userAdminService;

  @Autowired
  public EgressSumologicRemediationService(
      Clock clock,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserService userService,
      LeonardoApiClient leonardoNotebooksClient,
      EgressEventAuditor egressEventAuditor,
      EgressEventDao egressEventDao,
      SumologicEgressEventMapper sumologicEgressEventMapper,
      @Qualifier(SUMOLOGIC_JIRA_HANDLER_QUALIFIER) EgressJiraHandler egressJiraHandler,
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
    this.sumologicEgressEventMapper = sumologicEgressEventMapper;
    this.userAdminService = userAdminService;
  }

  @Override
  protected void sendEgressRemediationEmail(
      DbUser user, EgressRemediationAction action, DbEgressEvent event) throws MessagingException {
    // null for Jupyter
    String gkeServiceName =
        sumologicEgressEventMapper.toSumoLogicEvent(event).getSrcGkeServiceName();
    mailService.sendEgressRemediationEmail(user, action, gkeServiceName);
  }

  @Override
  protected void logEventToJira(DbEgressEvent event, EgressRemediationAction action)
      throws ApiException {
    egressJiraHandler.logEventToJira(event, action);
  }

  @Override
  protected boolean shouldSkipEgressEvent(DbEgressEvent event) {
    if (isCromwellApp(event)) {
      logger.info(
          String.format(
              "Skip egress event %d because this is triggered by Cromwell app and caused by GKE internal traffic",
              event.getEgressEventId()));
      return true;
    }
    if (isUserBypassedForLargeFileDownload(event)) {
      logger.info(
          String.format(
              "Skip egress event %d because user is bypassed for large file download",
              event.getEgressEventId()));
      return true;
    }
    return false;
  }

  private boolean isUserBypassedForLargeFileDownload(DbEgressEvent event) {
    return userAdminService.getCurrentEgressBypassWindow(event.getUser().getUserId()) != null
        && event.getEgressMegabytes() != null
        && event.getEgressMegabytes() < EGRESS_HARD_LIMIT_MB;
  }

  private boolean isCromwellApp(DbEgressEvent event) {
    String serviceName = sumologicEgressEventMapper.toSumoLogicEvent(event).getSrcGkeServiceName();
    return serviceName != null
        && appServiceNameToAppType(serviceName).equals(Optional.of(AppType.CROMWELL));
  }
}
