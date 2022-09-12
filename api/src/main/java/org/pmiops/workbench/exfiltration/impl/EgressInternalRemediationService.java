package org.pmiops.workbench.exfiltration.impl;

import java.time.Clock;
import javax.inject.Provider;
import javax.mail.MessagingException;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exfiltration.EgressRemediationAction;
import org.pmiops.workbench.exfiltration.EgressRemediationService;
import org.pmiops.workbench.exfiltration.jirahandler.EgressJiraHandler;
import org.pmiops.workbench.jira.ApiException;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("internal-remediation-service")
public class EgressInternalRemediationService extends EgressRemediationService {

  private final EgressJiraHandler egressJiraHandler;
  private final MailService mailService;

  @Autowired
  public EgressInternalRemediationService(
      Clock clock,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserService userService,
      LeonardoNotebooksClient leonardoNotebooksClient,
      EgressEventAuditor egressEventAuditor,
      EgressEventDao egressEventDao,
      @Qualifier("internal-jira-handler") EgressJiraHandler egressJiraHandler,
      MailService mailService) {
    super(
        clock,
        workbenchConfigProvider,
        userService,
        leonardoNotebooksClient,
        egressEventAuditor,
        egressEventDao);
    this.egressJiraHandler = egressJiraHandler;
    this.mailService = mailService;
  }

  @Override
  protected void sendEgressRemediationEmail(DbUser user, EgressRemediationAction action)
      throws MessagingException {
    mailService.sendInternalEgressRemediationEmail(user, action);
  }

  @Override
  protected void logEventToJira(DbEgressEvent event, EgressRemediationAction action)
      throws ApiException {
    egressJiraHandler.logEventToJira(event, action);
  }
}
