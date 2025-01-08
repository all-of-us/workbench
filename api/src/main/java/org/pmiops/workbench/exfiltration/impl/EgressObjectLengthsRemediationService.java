package org.pmiops.workbench.exfiltration.impl;

import static org.pmiops.workbench.exfiltration.ExfiltrationUtils.EGRESS_OBJECT_LENGTHS_SERVICE_QUALIFIER;

import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import java.time.Clock;
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
import org.pmiops.workbench.utils.mappers.EgressEventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service(EGRESS_OBJECT_LENGTHS_SERVICE_QUALIFIER)
public class EgressObjectLengthsRemediationService extends EgressRemediationService {

  private final EgressEventMapper egressEventMapper;
  private final EgressJiraHandler egressJiraHandler;
  private final MailService mailService;

  @Autowired
  public EgressObjectLengthsRemediationService(
      Clock clock,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserService userService,
      LeonardoApiClient leonardoNotebooksClient,
      EgressEventAuditor egressEventAuditor,
      EgressEventDao egressEventDao,
      EgressEventMapper egressEventMapper,
      @Qualifier(ExfiltrationUtils.OBJECT_LENGTHS_JIRA_HANDLER_QUALIFIER)
          EgressJiraHandler egressJiraHandler,
      MailService mailService) {
    super(
        clock,
        workbenchConfigProvider,
        userService,
        leonardoNotebooksClient,
        egressEventAuditor,
        egressEventDao);
    this.egressEventMapper = egressEventMapper;
    this.egressJiraHandler = egressJiraHandler;
    this.mailService = mailService;
  }

  @Override
  protected void sendEgressRemediationEmail(
      DbUser user, EgressRemediationAction action, DbEgressEvent event) throws MessagingException {
    disableUser(user);
    // null for Jupyter
    String gkeServiceName = egressEventMapper.toSumoLogicEvent(event).getSrcGkeServiceName();
    mailService.sendEgressRemediationEmail(user, action, gkeServiceName);
  }

  @Override
  protected void logEvent(DbEgressEvent event, EgressRemediationAction action) throws ApiException {
    egressJiraHandler.logEventToJira(event, action);
  }

  @Override
  protected boolean shouldSkipEgressEvent(DbEgressEvent event) {
    return false;
  }
}
