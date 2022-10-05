package org.pmiops.workbench.exfiltration;

public interface ExfiltrationConstants {

  long THRESHOLD_MB = 150 * 1024 * 1024; // 150MB

  String EGRESS_OBJECT_LENGTHS_SERVICE_QUALIFIER = "objectLengthsEgressService";
  String OBJECT_LENGTHS_JIRA_HANDLER_QUALIFIER = "objectLengthsJiraHandler";

  String EGRESS_SUMOLOGIC_SERVICE_QUALIFIER = "sumologicEgressService";
  String SUMOLOGIC_JIRA_HANDLER_QUALIFIER = "sumologicJiraHandler";
}
