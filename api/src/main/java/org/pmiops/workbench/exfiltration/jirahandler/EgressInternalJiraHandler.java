package org.pmiops.workbench.exfiltration.jirahandler;

import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.exfiltration.EgressRemediationAction;
import org.pmiops.workbench.jira.ApiException;
import org.springframework.stereotype.Service;

@Service("internal-jira-handler")
public class EgressInternalJiraHandler implements EgressJiraHandler {

  @Override
  public void logEventToJira(DbEgressEvent event, EgressRemediationAction action)
      throws ApiException {}
}
