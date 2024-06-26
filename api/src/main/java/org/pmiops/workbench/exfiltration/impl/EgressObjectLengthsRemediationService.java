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
import org.pmiops.workbench.user.UserAdminService;
import org.pmiops.workbench.utils.mappers.EgressEventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service(EGRESS_OBJECT_LENGTHS_SERVICE_QUALIFIER)
public class EgressObjectLengthsRemediationService extends EgressRemediationService {

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
      @Qualifier(ExfiltrationUtils.OBJECT_LENGTHS_JIRA_HANDLER_QUALIFIER)
          EgressJiraHandler egressJiraHandler,
      MailService mailService,
      EgressEventMapper egressEventMapper,
      UserAdminService userAdminService) {
    super(
        clock,
        workbenchConfigProvider,
        userService,
        leonardoNotebooksClient,
        egressEventAuditor,
        egressEventDao,
        egressEventMapper,
        userAdminService);
    this.egressJiraHandler = egressJiraHandler;
    this.mailService = mailService;
  }

  @Override
  protected void sendEgressRemediationEmail(
      DbUser user, EgressRemediationAction action, DbEgressEvent event) throws MessagingException {
    disableUser(user);
    mailService.sendFileLengthsEgressRemediationEmail(user, action);
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
