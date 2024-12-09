package org.pmiops.workbench.actionaudit.auditors;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.SumologicEgressEvent;
import org.pmiops.workbench.model.SumologicEgressEventRequest;
import org.pmiops.workbench.model.VwbEgressEventRequest;

/**
 * Auditor service which handles collecting audit logs for high-egress events. These events are
 * calculated by SumoLogic and routed through to the Workbench for logging and to take any automated
 * action in response to the event.
 */
public interface EgressEventAuditor {
  /**
   * Decorates a Sumologic-reported high-egress event with Workbench metadata and fires an audit
   * event log in the target workspace.
   */
  void fireEgressEvent(SumologicEgressEvent event);

  /**
   * Decorates a Verily Workbench high-egress event with Workbench metadata and fires an audit event
   * log in the target workspace.
   */
  void fireVwbEgressEvent(VwbEgressEventRequest event, DbUser dbUser);

  /**
   * Decorates a Sumologic-reported high-egress event with Workbench metadata and fires an audit
   * event log in the target workspace for the specified user.
   */
  void fireEgressEventForUser(SumologicEgressEvent event, DbUser user);

  /**
   * Decorates a high-egress event with remediation metadata and fires an audit event log in the
   * target workspace.
   */
  void fireRemediateEgressEvent(
      DbEgressEvent dbEvent, WorkbenchConfig.EgressAlertRemediationPolicy.Escalation escalation);

  /** Fires an audit log event for an admin update to an egress event. */
  void fireAdminEditEgressEvent(DbEgressEvent previousEvent, DbEgressEvent updatedEvent);

  /**
   * Fires an audit event log tracking when a Sumologic-reported high-egress event request could not
   * successfully be parsed.
   */
  void fireFailedToParseEgressEventRequest(SumologicEgressEventRequest request);

  /**
   * Fires an audit event log tracking when a Sumologic-reported high-egress event request did not
   * have a valid API key.
   */
  void fireBadApiKey(String apiKey, SumologicEgressEventRequest request);
}
